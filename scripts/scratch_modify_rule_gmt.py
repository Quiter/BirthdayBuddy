import sqlite3

db_path = "settings_database_emu_after"
conn = sqlite3.connect(db_path)
cursor = conn.cursor()
try:
    cursor.execute("UPDATE notification_rules SET hour = 6, minute = 15 WHERE id = 1;")
    conn.commit()
    print("Successfully updated rule to 06:15!")
    
    cursor.execute("SELECT * FROM notification_rules;")
    print("Current rules in DB:")
    for row in cursor.fetchall():
        print(row)
except Exception as e:
    print(f"Error: {e}")
finally:
    conn.close()
