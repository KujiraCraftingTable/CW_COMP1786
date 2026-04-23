package com.example.expensetrackerapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.expensetrackerapp.model.Expense;
import com.example.expensetrackerapp.model.Project;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "ExpenseTracker.db";
    private static final int DATABASE_VERSION = 2;

    // ── Projects table ─────────────────────────────────────────────────────────
    public static final String TABLE_PROJECTS       = "projects";
    public static final String COL_PROJECT_ID       = "id";
    public static final String COL_PROJECT_CODE     = "project_code";
    public static final String COL_PROJECT_NAME     = "project_name";
    public static final String COL_DESCRIPTION      = "description";
    public static final String COL_START_DATE       = "start_date";
    public static final String COL_END_DATE         = "end_date";
    public static final String COL_MANAGER          = "manager";
    public static final String COL_STATUS           = "status";
    public static final String COL_BUDGET           = "budget";
    public static final String COL_SPECIAL_REQS     = "special_requirements";
    public static final String COL_CLIENT_DEPT      = "client_department";
    public static final String COL_PRIORITY         = "priority";
    public static final String COL_NOTES            = "notes";
    public static final String COL_CREATED_AT       = "created_at";
    /** 0 = pending upload / modified since last sync, 1 = synced with server */
    public static final String COL_IS_SYNCED        = "is_synced";

    // ── Expenses table ─────────────────────────────────────────────────────────
    public static final String TABLE_EXPENSES            = "expenses";
    public static final String COL_EXPENSE_ID            = "id";
    public static final String COL_EXPENSE_PROJECT_ID    = "project_id";
    public static final String COL_EXPENSE_CODE          = "expense_code";
    public static final String COL_EXPENSE_DATE          = "expense_date";
    public static final String COL_AMOUNT                = "amount";
    public static final String COL_CURRENCY              = "currency";
    public static final String COL_EXPENSE_TYPE          = "expense_type";
    public static final String COL_PAYMENT_METHOD        = "payment_method";
    public static final String COL_CLAIMANT              = "claimant";
    public static final String COL_PAYMENT_STATUS        = "payment_status";
    public static final String COL_EXPENSE_DESC          = "description";
    public static final String COL_LOCATION              = "location";
    public static final String COL_RECEIPT_NUMBER        = "receipt_number";
    public static final String COL_EXPENSE_CREATED       = "created_at";

    // ── CREATE statements ──────────────────────────────────────────────────────
    private static final String CREATE_TABLE_PROJECTS =
            "CREATE TABLE " + TABLE_PROJECTS + " ("
            + COL_PROJECT_ID    + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COL_PROJECT_CODE  + " TEXT NOT NULL, "
            + COL_PROJECT_NAME  + " TEXT NOT NULL, "
            + COL_DESCRIPTION   + " TEXT NOT NULL, "
            + COL_START_DATE    + " TEXT NOT NULL, "
            + COL_END_DATE      + " TEXT NOT NULL, "
            + COL_MANAGER       + " TEXT NOT NULL, "
            + COL_STATUS        + " TEXT NOT NULL, "
            + COL_BUDGET        + " REAL NOT NULL, "
            + COL_SPECIAL_REQS  + " TEXT, "
            + COL_CLIENT_DEPT   + " TEXT, "
            + COL_PRIORITY      + " TEXT NOT NULL, "
            + COL_NOTES         + " TEXT, "
            + COL_CREATED_AT    + " TEXT, "
            + COL_IS_SYNCED     + " INTEGER DEFAULT 0"
            + ")";

    private static final String CREATE_TABLE_EXPENSES =
            "CREATE TABLE " + TABLE_EXPENSES + " ("
            + COL_EXPENSE_ID         + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COL_EXPENSE_PROJECT_ID + " INTEGER NOT NULL, "
            + COL_EXPENSE_CODE       + " TEXT NOT NULL, "
            + COL_EXPENSE_DATE       + " TEXT NOT NULL, "
            + COL_AMOUNT             + " REAL NOT NULL, "
            + COL_CURRENCY           + " TEXT NOT NULL, "
            + COL_EXPENSE_TYPE       + " TEXT NOT NULL, "
            + COL_PAYMENT_METHOD     + " TEXT NOT NULL, "
            + COL_CLAIMANT           + " TEXT NOT NULL, "
            + COL_PAYMENT_STATUS     + " TEXT NOT NULL, "
            + COL_EXPENSE_DESC       + " TEXT, "
            + COL_LOCATION           + " TEXT, "
            + COL_RECEIPT_NUMBER     + " TEXT, "
            + COL_EXPENSE_CREATED    + " TEXT, "
            + "FOREIGN KEY(" + COL_EXPENSE_PROJECT_ID + ") REFERENCES "
            + TABLE_PROJECTS + "(" + COL_PROJECT_ID + ")"
            + ")";

    // ── Singleton ──────────────────────────────────────────────────────────────
    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_PROJECTS);
        db.execSQL(CREATE_TABLE_EXPENSES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // v1 → v2: add is_synced column; existing rows default to 0 (unsynced)
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_PROJECTS
                    + " ADD COLUMN " + COL_IS_SYNCED + " INTEGER DEFAULT 0");
        }
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PROJECT OPERATIONS
    // ══════════════════════════════════════════════════════════════════════════

    public long insertProject(Project p) {
        p.setIsSynced(0); // always unsynced on creation
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = projectToValues(p);
        long id = db.insert(TABLE_PROJECTS, null, v);
        db.close();
        return id;
    }

    public int updateProject(Project p) {
        p.setIsSynced(0); // mark unsynced whenever local data changes
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = projectToValues(p);
        int rows = db.update(TABLE_PROJECTS, v,
                COL_PROJECT_ID + "=?", new String[]{String.valueOf(p.getId())});
        db.close();
        return rows;
    }

    public void deleteProject(int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_EXPENSES, COL_EXPENSE_PROJECT_ID + "=?",
                new String[]{String.valueOf(id)});
        db.delete(TABLE_PROJECTS, COL_PROJECT_ID + "=?",
                new String[]{String.valueOf(id)});
        db.close();
    }

    public List<Project> getAllProjects() {
        List<Project> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_PROJECTS, null, null, null, null, null,
                COL_PROJECT_NAME + " ASC");
        if (c.moveToFirst()) {
            do { list.add(cursorToProject(c)); } while (c.moveToNext());
        }
        c.close();
        db.close();
        return list;
    }

    public Project getProjectById(int id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_PROJECTS, null,
                COL_PROJECT_ID + "=?", new String[]{String.valueOf(id)},
                null, null, null);
        Project project = null;
        if (c.moveToFirst()) project = cursorToProject(c);
        c.close();
        db.close();
        return project;
    }

    /** Basic search: name or description LIKE %query% */
    public List<Project> searchProjects(String query) {
        List<Project> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String like = "%" + query + "%";
        String sel = COL_PROJECT_NAME + " LIKE ? OR " + COL_DESCRIPTION + " LIKE ?";
        Cursor c = db.query(TABLE_PROJECTS, null, sel,
                new String[]{like, like}, null, null, COL_PROJECT_NAME + " ASC");
        if (c.moveToFirst()) {
            do { list.add(cursorToProject(c)); } while (c.moveToNext());
        }
        c.close();
        db.close();
        return list;
    }

    /** Advanced search: filter by date, status, and/or owner (all optional) */
    public List<Project> advancedSearchProjects(String dateFilter,
                                                 String statusFilter,
                                                 String ownerFilter) {
        List<Project> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        StringBuilder sel = new StringBuilder("1=1");
        List<String> args = new ArrayList<>();

        if (dateFilter != null && !dateFilter.trim().isEmpty()) {
            sel.append(" AND (").append(COL_START_DATE).append(" LIKE ?")
               .append(" OR ").append(COL_END_DATE).append(" LIKE ?)");
            String like = "%" + dateFilter.trim() + "%";
            args.add(like);
            args.add(like);
        }
        if (statusFilter != null && !statusFilter.trim().isEmpty()) {
            sel.append(" AND ").append(COL_STATUS).append("=?");
            args.add(statusFilter.trim());
        }
        if (ownerFilter != null && !ownerFilter.trim().isEmpty()) {
            sel.append(" AND ").append(COL_MANAGER).append(" LIKE ?");
            args.add("%" + ownerFilter.trim() + "%");
        }

        String[] selArgs = args.isEmpty() ? null : args.toArray(new String[0]);
        Cursor c = db.query(TABLE_PROJECTS, null, sel.toString(), selArgs,
                null, null, COL_PROJECT_NAME + " ASC");
        if (c.moveToFirst()) {
            do { list.add(cursorToProject(c)); } while (c.moveToNext());
        }
        c.close();
        db.close();
        return list;
    }

    public void resetDatabase() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_EXPENSES, null, null);
        db.delete(TABLE_PROJECTS, null, null);
        db.close();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // EXPENSE OPERATIONS
    // ══════════════════════════════════════════════════════════════════════════

    public long insertExpense(Expense e) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = expenseToValues(e);
        long id = db.insert(TABLE_EXPENSES, null, v);
        markProjectUnsyncedInternal(db, e.getProjectId());
        db.close();
        return id;
    }

    public int updateExpense(Expense e) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = expenseToValues(e);
        int rows = db.update(TABLE_EXPENSES, v,
                COL_EXPENSE_ID + "=?", new String[]{String.valueOf(e.getId())});
        markProjectUnsyncedInternal(db, e.getProjectId());
        db.close();
        return rows;
    }

    public void deleteExpense(int id) {
        SQLiteDatabase db = getWritableDatabase();
        // find parent project before deleting
        Cursor c = db.query(TABLE_EXPENSES,
                new String[]{COL_EXPENSE_PROJECT_ID},
                COL_EXPENSE_ID + "=?", new String[]{String.valueOf(id)},
                null, null, null);
        if (c.moveToFirst()) {
            int projectId = c.getInt(0);
            c.close();
            db.delete(TABLE_EXPENSES, COL_EXPENSE_ID + "=?",
                    new String[]{String.valueOf(id)});
            markProjectUnsyncedInternal(db, projectId);
        } else {
            c.close();
            db.delete(TABLE_EXPENSES, COL_EXPENSE_ID + "=?",
                    new String[]{String.valueOf(id)});
        }
        db.close();
    }

    public List<Expense> getExpensesByProject(int projectId) {
        List<Expense> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_EXPENSES, null,
                COL_EXPENSE_PROJECT_ID + "=?",
                new String[]{String.valueOf(projectId)},
                null, null, COL_EXPENSE_DATE + " DESC");
        if (c.moveToFirst()) {
            do { list.add(cursorToExpense(c)); } while (c.moveToNext());
        }
        c.close();
        db.close();
        return list;
    }

    public Expense getExpenseByCode(int projectId, String expenseCode) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_EXPENSES, null,
                COL_EXPENSE_PROJECT_ID + "=? AND " + COL_EXPENSE_CODE + "=?",
                new String[]{String.valueOf(projectId), expenseCode},
                null, null, null);
        Expense expense = null;
        if (c.moveToFirst()) expense = cursorToExpense(c);
        c.close();
        db.close();
        return expense;
    }

    public double getTotalExpensesByProject(int projectId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT SUM(" + COL_AMOUNT + ") FROM " + TABLE_EXPENSES
                + " WHERE " + COL_EXPENSE_PROJECT_ID + "=?",
                new String[]{String.valueOf(projectId)});
        double total = 0;
        if (c.moveToFirst() && !c.isNull(0)) total = c.getDouble(0);
        c.close();
        db.close();
        return total;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SYNC OPERATIONS
    // ══════════════════════════════════════════════════════════════════════════

    /** Mark one project as successfully synced with the server. */
    public void markProjectSynced(int projectId) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(COL_IS_SYNCED, 1);
        db.update(TABLE_PROJECTS, v,
                COL_PROJECT_ID + "=?", new String[]{String.valueOf(projectId)});
        db.close();
    }

    /** Mark all projects as synced (called after a full successful upload). */
    public void markAllProjectsSynced() {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(COL_IS_SYNCED, 1);
        db.update(TABLE_PROJECTS, v, null, null);
        db.close();
    }

    /** Returns the number of projects that have not yet been uploaded or were modified. */
    public int getUnsyncedProjectCount() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_PROJECTS
                + " WHERE " + COL_IS_SYNCED + "=0", null);
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        db.close();
        return count;
    }

    // ── Internal helper (called with an already-open writable DB) ─────────────
    private void markProjectUnsyncedInternal(SQLiteDatabase db, int projectId) {
        ContentValues v = new ContentValues();
        v.put(COL_IS_SYNCED, 0);
        db.update(TABLE_PROJECTS, v,
                COL_PROJECT_ID + "=?", new String[]{String.valueOf(projectId)});
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private ContentValues projectToValues(Project p) {
        ContentValues v = new ContentValues();
        v.put(COL_PROJECT_CODE,  p.getProjectCode());
        v.put(COL_PROJECT_NAME,  p.getProjectName());
        v.put(COL_DESCRIPTION,   p.getDescription());
        v.put(COL_START_DATE,    p.getStartDate());
        v.put(COL_END_DATE,      p.getEndDate());
        v.put(COL_MANAGER,       p.getManager());
        v.put(COL_STATUS,        p.getStatus());
        v.put(COL_BUDGET,        p.getBudget());
        v.put(COL_SPECIAL_REQS,  p.getSpecialRequirements());
        v.put(COL_CLIENT_DEPT,   p.getClientDepartment());
        v.put(COL_PRIORITY,      p.getPriority());
        v.put(COL_NOTES,         p.getNotes());
        v.put(COL_CREATED_AT,    p.getCreatedAt());
        v.put(COL_IS_SYNCED,     p.getIsSynced());
        return v;
    }

    private ContentValues expenseToValues(Expense e) {
        ContentValues v = new ContentValues();
        v.put(COL_EXPENSE_PROJECT_ID, e.getProjectId());
        v.put(COL_EXPENSE_CODE,       e.getExpenseCode());
        v.put(COL_EXPENSE_DATE,       e.getExpenseDate());
        v.put(COL_AMOUNT,             e.getAmount());
        v.put(COL_CURRENCY,           e.getCurrency());
        v.put(COL_EXPENSE_TYPE,       e.getExpenseType());
        v.put(COL_PAYMENT_METHOD,     e.getPaymentMethod());
        v.put(COL_CLAIMANT,           e.getClaimant());
        v.put(COL_PAYMENT_STATUS,     e.getPaymentStatus());
        v.put(COL_EXPENSE_DESC,       e.getDescription());
        v.put(COL_LOCATION,           e.getLocation());
        v.put(COL_RECEIPT_NUMBER,     e.getReceiptNumber());
        v.put(COL_EXPENSE_CREATED,    e.getCreatedAt());
        return v;
    }

    private Project cursorToProject(Cursor c) {
        Project p = new Project();
        p.setId(c.getInt(c.getColumnIndexOrThrow(COL_PROJECT_ID)));
        p.setProjectCode(c.getString(c.getColumnIndexOrThrow(COL_PROJECT_CODE)));
        p.setProjectName(c.getString(c.getColumnIndexOrThrow(COL_PROJECT_NAME)));
        p.setDescription(c.getString(c.getColumnIndexOrThrow(COL_DESCRIPTION)));
        p.setStartDate(c.getString(c.getColumnIndexOrThrow(COL_START_DATE)));
        p.setEndDate(c.getString(c.getColumnIndexOrThrow(COL_END_DATE)));
        p.setManager(c.getString(c.getColumnIndexOrThrow(COL_MANAGER)));
        p.setStatus(c.getString(c.getColumnIndexOrThrow(COL_STATUS)));
        p.setBudget(c.getDouble(c.getColumnIndexOrThrow(COL_BUDGET)));
        p.setSpecialRequirements(c.getString(c.getColumnIndexOrThrow(COL_SPECIAL_REQS)));
        p.setClientDepartment(c.getString(c.getColumnIndexOrThrow(COL_CLIENT_DEPT)));
        p.setPriority(c.getString(c.getColumnIndexOrThrow(COL_PRIORITY)));
        p.setNotes(c.getString(c.getColumnIndexOrThrow(COL_NOTES)));
        p.setCreatedAt(c.getString(c.getColumnIndexOrThrow(COL_CREATED_AT)));
        int syncIdx = c.getColumnIndex(COL_IS_SYNCED);
        p.setIsSynced(syncIdx >= 0 ? c.getInt(syncIdx) : 0);
        return p;
    }

    private Expense cursorToExpense(Cursor c) {
        Expense e = new Expense();
        e.setId(c.getInt(c.getColumnIndexOrThrow(COL_EXPENSE_ID)));
        e.setProjectId(c.getInt(c.getColumnIndexOrThrow(COL_EXPENSE_PROJECT_ID)));
        e.setExpenseCode(c.getString(c.getColumnIndexOrThrow(COL_EXPENSE_CODE)));
        e.setExpenseDate(c.getString(c.getColumnIndexOrThrow(COL_EXPENSE_DATE)));
        e.setAmount(c.getDouble(c.getColumnIndexOrThrow(COL_AMOUNT)));
        e.setCurrency(c.getString(c.getColumnIndexOrThrow(COL_CURRENCY)));
        e.setExpenseType(c.getString(c.getColumnIndexOrThrow(COL_EXPENSE_TYPE)));
        e.setPaymentMethod(c.getString(c.getColumnIndexOrThrow(COL_PAYMENT_METHOD)));
        e.setClaimant(c.getString(c.getColumnIndexOrThrow(COL_CLAIMANT)));
        e.setPaymentStatus(c.getString(c.getColumnIndexOrThrow(COL_PAYMENT_STATUS)));
        e.setDescription(c.getString(c.getColumnIndexOrThrow(COL_EXPENSE_DESC)));
        e.setLocation(c.getString(c.getColumnIndexOrThrow(COL_LOCATION)));
        e.setReceiptNumber(c.getString(c.getColumnIndexOrThrow(COL_RECEIPT_NUMBER)));
        e.setCreatedAt(c.getString(c.getColumnIndexOrThrow(COL_EXPENSE_CREATED)));
        return e;
    }
}
