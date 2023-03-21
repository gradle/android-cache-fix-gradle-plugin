package org.gradle.android.writer

class JavaRoom {

    static String entity(String packageName) {
        return """
                package ${packageName};

                import androidx.room.ColumnInfo;
                import androidx.room.Entity;
                import androidx.room.PrimaryKey;

                @Entity(tableName = "user")
                public class JavaUser {
                    @PrimaryKey
                    public int uid;

                    @ColumnInfo(name = "first_name")
                    public String firstName;

                    @ColumnInfo(name = "last_name")
                    public String lastName;

                    @ColumnInfo(name = "last_update")
                    public int lastUpdate;
                }
            """.stripIndent()
    }

    static String dao(String packageName) {
        return """
                package ${packageName};

                import androidx.room.Dao;
                import androidx.room.Query;
                import androidx.room.Insert;
                import androidx.room.Delete;

                import java.util.List;

                @Dao
                public interface JavaUserDao {
                    @Query("SELECT * FROM user")
                    List<JavaUser> getAll();

                    @Query("SELECT * FROM user WHERE uid IN (:userIds)")
                    List<JavaUser> loadAllByIds(int[] userIds);

                    @Query("SELECT * FROM user WHERE first_name LIKE :first AND " +
                           "last_name LIKE :last LIMIT 1")
                    JavaUser findByName(String first, String last);

                    @Insert
                    void insertAll(JavaUser... users);

                    @Delete
                    void delete(JavaUser user);
                }
            """.stripIndent()
    }

    static String database(String packageName) {
        """
                package ${packageName};

                import androidx.room.Database;
                import androidx.room.Room;
                import androidx.room.RoomDatabase;
                import androidx.room.migration.Migration;
                import androidx.sqlite.db.SupportSQLiteDatabase;
                import android.content.Context;

                @Database(entities = {JavaUser.class}, version = 2, exportSchema = true)
                public abstract class AppDatabase extends RoomDatabase {
                    private static AppDatabase INSTANCE;
                    private static final Object sLock = new Object();

                    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
                        @Override
                        public void migrate(SupportSQLiteDatabase database) {
                            database.execSQL("ALTER TABLE Users "
                                    + " ADD COLUMN last_update INTEGER");
                        }
                    };

                    public abstract JavaUserDao javaUserDao();

                    public static AppDatabase getInstance(Context context) {
                        synchronized (sLock) {
                            if (INSTANCE == null) {
                                INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                        AppDatabase.class, "Sample.db")
                                        .addMigrations(MIGRATION_1_2)
                                        .build();
                            }
                            return INSTANCE;
                        }
                    }
                }
            """.stripIndent()
    }

    static String legacySchemaJson() {
        return """
                        {
                  "formatVersion": 1,
                  "database": {
                    "version": 1,
                    "identityHash": "ce7bbbf6ddf39482eddc7248f4f61e8a",
                    "entities": [
                      {
                        "tableName": "user",
                        "createSql": "CREATE TABLE IF NOT EXISTS `${TABLE_NAME}` (`uid` INTEGER NOT NULL, `first_name` TEXT, `last_name` TEXT, PRIMARY KEY(`uid`))",
                        "fields": [
                          {
                            "fieldPath": "uid",
                            "columnName": "uid",
                            "affinity": "INTEGER",
                            "notNull": true
                          },
                          {
                            "fieldPath": "firstName",
                            "columnName": "first_name",
                            "affinity": "TEXT",
                            "notNull": false
                          },
                          {
                            "fieldPath": "lastName",
                            "columnName": "last_name",
                            "affinity": "TEXT",
                            "notNull": false
                          }
                        ],
                        "primaryKey": {
                          "columnNames": [
                            "uid"
                          ],
                          "autoGenerate": false
                        },
                        "indices": [],
                        "foreignKeys": []
                      }
                    ],
                    "views": [],
                    "setupQueries": [
                      "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)",
                      "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'ce7bbbf6ddf39482eddc7248f4f61e8a')"
                    ]
                  }
                }
        """.stripIndent()
    }
}
