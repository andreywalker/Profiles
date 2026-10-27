package com.example.profiles.data
import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Group::class, Person::class, FieldDef::class,
        FieldValue::class, ReminderLog::class, Setting::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDb : RoomDatabase() {
    abstract fun personDao(): PersonDao
    abstract fun groupDao(): GroupDao
    abstract fun fieldDao(): FieldDao
    abstract fun reminderDao(): ReminderDao
    abstract fun settingDao(): SettingDao

    companion object {
        fun create(context: Context): AppDb =
            Room.databaseBuilder(context, AppDb::class.java, "profiles.db")
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        // runs once, when the SQLite file is first created
                        db.execSQL("INSERT INTO person_groups (name, isDefault) VALUES ('No group', 1)")
                        db.execSQL("""INSERT INTO field_defs (name, type, deletable, position, builtInKey) VALUES
                            ('Favorite color', 'TEXT', 1, 0, 'favorite_color'),
                            ('Wishlist',       'TEXT', 1, 1, 'wishlist'),
                            ('Facts',          'TEXT', 0, 0, 'facts')""")
                        db.execSQL("""INSERT INTO settings (`key`, value) VALUES
                            ('theme', 'DARK'), ('language', 'system')""")
                    }
                })
                .build()
    }
}