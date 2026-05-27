import os
import sqlite3
import sys

sys.stdout.reconfigure(encoding='utf-8')

def inspect_db(db_path, query, label):
    if not os.path.exists(db_path):
        print(f"File not found: {db_path}")
        return
    print(f"=== {label} ({db_path}) ===")
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()
    try:
        cursor.execute(query)
        rows = cursor.fetchall()
        colnames = [desc[0] for desc in cursor.description]
        print(" | ".join(colnames))
        print("-" * 50)
        for row in rows:
            print(" | ".join(str(val) for val in row))
    except Exception as e:
        print(f"Error querying {label}: {e}")
    finally:
        conn.close()
    print()

inspect_db("settings_database_phone", "SELECT * FROM notification_rules;", "Notification Rules (Phone)")
inspect_db("settings_database_phone", "SELECT * FROM app_settings;", "App Settings (Phone)")
inspect_db("birthday_database_phone", "SELECT localId, fullName, birthday, lookupKey FROM contacts WHERE birthday LIKE '%-05-27';", "Contacts with birthday today (Phone)")
inspect_db("birthday_database_phone", "SELECT * FROM pending_notifications;", "Pending Notifications (Phone)")
