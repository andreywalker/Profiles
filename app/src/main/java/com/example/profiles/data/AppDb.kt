package com.example.profiles.data
import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Group::class, Person::class, FieldDef::class, FieldValue::class,
        ReminderLog::class, Setting::class, HistoryEntry::class],
    version = 3,          // was 1; bump it again every time you change an entity
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDb : RoomDatabase() {
    abstract fun personDao(): PersonDao
    abstract fun groupDao(): GroupDao
    abstract fun fieldDao(): FieldDao
    abstract fun reminderDao(): ReminderDao
    abstract fun settingDao(): SettingDao
    abstract fun historyDao(): HistoryDao

    companion object {
        fun create(context: Context): AppDb =
            Room.databaseBuilder(context, AppDb::class.java, "profiles.db")
                .fallbackToDestructiveMigration(dropAllTables = true)
                .addCallback(object : Callback() {
                    // onOpen runs on every start, also after a destructive migration,
                    // unlike onCreate. Each insert only adds what's missing.
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        seed(db)
                    }
                })
                .build()

        private fun seed(db: SupportSQLiteDatabase) {
            // "No group" — only if there's no default group yet
            db.execSQL("""
            INSERT INTO person_groups (name, isDefault)
            SELECT 'No group', 1
            WHERE NOT EXISTS (SELECT 1 FROM person_groups WHERE isDefault = 1)
        """)

            // Built-in fields — only on a fresh database.
            // Facts can't be deleted, so if it exists, seeding already happened
            // (this keeps Favorite color / Wishlist deleted if the user removed them).
            db.execSQL("""
            INSERT INTO field_defs (name, type, deletable, position, builtInKey)
            SELECT 'Favorite color', 'TEXT', 1, 0, 'favorite_color'
            WHERE NOT EXISTS (SELECT 1 FROM field_defs WHERE builtInKey = 'facts')
        """)
            db.execSQL("""
            INSERT INTO field_defs (name, type, deletable, position, builtInKey)
            SELECT 'Wishlist', 'TEXT', 1, 1, 'wishlist'
            WHERE NOT EXISTS (SELECT 1 FROM field_defs WHERE builtInKey = 'facts')
        """)
            db.execSQL("""
            INSERT INTO field_defs (name, type, deletable, position, builtInKey)
            SELECT 'Facts', 'TEXT', 0, 0, 'facts'
            WHERE NOT EXISTS (SELECT 1 FROM field_defs WHERE builtInKey = 'facts')
        """)

            // Default settings — OR IGNORE keeps the user's choice if it's already saved
            db.execSQL("INSERT OR IGNORE INTO settings (`key`, value) VALUES ('theme', 'DARK')")
            db.execSQL("INSERT OR IGNORE INTO settings (`key`, value) VALUES ('language', 'system')")
        }
    }
}