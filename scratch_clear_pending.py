import sqlite3

db_path = "birthday_database_emu_after"
conn = sqlite3.connect(db_path)
cursor = conn.cursor()
try:
    cursor.execute("DELETE FROM pending_notifications;")
    conn.commit()
    print("Successfully cleared pending_notifications!")
except Exception as e:
    print(f"Error: {e}")
finally:
    conn.close()
