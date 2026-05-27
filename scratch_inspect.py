import os
import sqlite3
import sys

# Force output encoding to utf-8 to avoid encoding crashes on Windows terminal
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
            # Safely convert to string and print
            print(" | ".join(str(val) for val in row))
    except Exception as e:
        print(f"Error querying {label}: {e}")
    finally:
        conn.close()
    print()

# Inspect settings_database tables
inspect_db("settings_database", "SELECT name FROM sqlite_master WHERE type='table';", "Tables in Settings DB")
inspect_db("settings_database", "SELECT * FROM notification_rules;", "Notification Rules")
inspect_db("settings_database", "SELECT * FROM app_settings;", "App Settings")

# Inspect birthday_database tables
inspect_db("birthday_database", "SELECT name FROM sqlite_master WHERE type='table';", "Tables in Birthday DB")
inspect_db("birthday_database", "SELECT localId, fullName, birthday, lookupKey FROM contacts;", "Contacts (subset)")
inspect_db("birthday_database", "SELECT * FROM pending_notifications;", "Pending Notifications")
