/**
 * Simple Express server for COMP1786 Expense Tracker
 *
 * Endpoints:
 *   GET  /                        → health check
 *   GET  /projects                → return all stored projects (Flutter app)
 *   POST /upload                  → receive projects from Android Admin app
 *   POST /expenses                → add an expense to a project (Flutter app)
 *   DELETE /expenses/:projectCode/:expenseCode → remove an expense (Flutter app)
 *
 * Data is persisted to db.json so it survives server restarts.
 *
 * Usage:
 *   cd server
 *   npm install
 *   node server.js
 *
 * The server runs on http://localhost:3000
 * From the Android emulator, use http://10.0.2.2:3000
 */

const express = require('express');
const fs      = require('fs');
const path    = require('path');

const app     = express();
const PORT    = 3000;
const DB_FILE = path.join(__dirname, 'db.json');

app.use(express.json({ limit: '10mb' }));

// ── Persistent storage ────────────────────────────────────────────────────────

function loadData() {
  try {
    if (fs.existsSync(DB_FILE)) {
      const raw = fs.readFileSync(DB_FILE, 'utf8');
      const parsed = JSON.parse(raw);
      console.log(`[DB] Loaded ${parsed.projects?.length ?? 0} project(s) from db.json`);
      return parsed.projects || [];
    }
  } catch (err) {
    console.error('[DB] Failed to load db.json, starting fresh:', err.message);
  }
  return [];
}

function saveData(projects) {
  try {
    fs.writeFileSync(DB_FILE, JSON.stringify({ projects }, null, 2), 'utf8');
  } catch (err) {
    console.error('[DB] Failed to save db.json:', err.message);
  }
}

// Load on startup
let storedProjects = loadData();

// ── CORS (allow all origins for development) ──────────────────────────────────
app.use((req, res, next) => {
  res.setHeader('Access-Control-Allow-Origin',  '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Accept');
  if (req.method === 'OPTIONS') return res.sendStatus(200);
  next();
});

// ── Health check ──────────────────────────────────────────────────────────────
app.get('/', (req, res) => {
  res.json({
    status:   'ok',
    message:  'Expense Tracker Server is running',
    projects: storedProjects.length,
    expenses: storedProjects.reduce((s, p) => s + (p.expenses?.length ?? 0), 0),
  });
});

// ── GET /projects → Flutter user app fetches this ─────────────────────────────
app.get('/projects', (req, res) => {
  console.log(`[GET /projects] Returning ${storedProjects.length} projects`);
  res.json({ projects: storedProjects });
});

// ── POST /upload → Android Admin app uploads here ─────────────────────────────
app.post('/upload', (req, res) => {
  try {
    const payload = req.body;

    if (!payload || !Array.isArray(payload.projects)) {
      return res.status(400).json({ error: 'Invalid payload: expected { projects: [...] }' });
    }

    // Merge/replace projects by projectCode
    payload.projects.forEach(incoming => {
      const idx = storedProjects.findIndex(p => p.projectCode === incoming.projectCode);
      if (idx >= 0) {
        // Keep expenses added by Flutter app that admin doesn't know about
        const flutterOnlyExpenses = (storedProjects[idx].expenses || []).filter(fe =>
          !(incoming.expenses || []).some(ae => ae.expenseCode === fe.expenseCode)
        );
        storedProjects[idx] = incoming;
        storedProjects[idx].expenses = [
          ...(incoming.expenses || []),
          ...flutterOnlyExpenses,
        ];
      } else {
        storedProjects.push(incoming);
      }
    });

    saveData(storedProjects);

    const totalExpenses = storedProjects.reduce(
      (sum, p) => sum + (p.expenses?.length ?? 0), 0
    );

    console.log(
      `[POST /upload] Received ${payload.projects.length} project(s).` +
      ` Total stored: ${storedProjects.length} projects, ${totalExpenses} expenses.`
    );

    res.json({
      success: true,
      message: `Received ${payload.projects.length} project(s) successfully`,
      total:   storedProjects.length,
    });
  } catch (err) {
    console.error('[POST /upload] Error:', err.message);
    res.status(500).json({ error: err.message });
  }
});

// ── POST /expenses → Flutter user app adds a new expense ──────────────────────
app.post('/expenses', (req, res) => {
  try {
    const { projectCode, expense } = req.body;

    if (!projectCode || !expense) {
      return res.status(400).json({ error: 'Missing projectCode or expense' });
    }

    const project = storedProjects.find(p => p.projectCode === projectCode);
    if (!project) {
      return res.status(404).json({
        error: `Project "${projectCode}" not found on server. ` +
               `Available: ${storedProjects.map(p => p.projectCode).join(', ') || 'none'}`
      });
    }

    if (!project.expenses) project.expenses = [];

    // Check for duplicate expense code
    const existing = project.expenses.findIndex(e => e.expenseCode === expense.expenseCode);
    if (existing >= 0) {
      project.expenses[existing] = expense; // update if duplicate
    } else {
      expense.createdAt = expense.createdAt || new Date().toISOString().slice(0, 10);
      project.expenses.push(expense);
    }
    project.expenseCount = project.expenses.length;

    saveData(storedProjects);

    console.log(
      `[POST /expenses] Added/updated expense "${expense.expenseCode}" ` +
      `on project "${projectCode}". Total expenses: ${project.expenses.length}`
    );

    res.json({ success: true, expense });
  } catch (err) {
    console.error('[POST /expenses] Error:', err.message);
    res.status(500).json({ error: err.message });
  }
});

// ── DELETE /expenses/:projectCode/:expenseCode ─────────────────────────────────
app.delete('/expenses/:projectCode/:expenseCode', (req, res) => {
  const { projectCode, expenseCode } = req.params;
  const project = storedProjects.find(p => p.projectCode === projectCode);

  if (!project) {
    return res.status(404).json({ error: `Project "${projectCode}" not found` });
  }

  const before = project.expenses?.length ?? 0;
  project.expenses = (project.expenses ?? []).filter(e => e.expenseCode !== expenseCode);
  project.expenseCount = project.expenses.length;

  saveData(storedProjects);

  const removed = before - project.expenses.length;
  console.log(`[DELETE /expenses] Removed ${removed} expense(s) from "${projectCode}"`);
  res.json({ success: true, removed });
});

// ── DELETE /projects/:projectCode ─────────────────────────────────────────────
app.delete('/projects/:projectCode', (req, res) => {
  const { projectCode } = req.params;
  const before = storedProjects.length;
  storedProjects = storedProjects.filter(p => p.projectCode !== projectCode);

  if (storedProjects.length === before) {
    return res.status(404).json({ error: `Project "${projectCode}" not found` });
  }

  saveData(storedProjects);
  console.log(`[DELETE /projects] Removed project "${projectCode}"`);
  res.json({ success: true });
});

// ── Start ──────────────────────────────────────────────────────────────────────
app.listen(PORT, () => {
  console.log('');
  console.log('  ✅  Expense Tracker Server started');
  console.log(`  📡  Listening on http://localhost:${PORT}`);
  console.log(`  📱  Android emulator: http://10.0.2.2:${PORT}`);
  console.log(`  💾  Data file: ${DB_FILE}`);
  console.log('');
});
