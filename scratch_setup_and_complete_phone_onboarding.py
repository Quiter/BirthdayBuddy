import subprocess
import os
import sqlite3
import re
import sys
import time

sys.stdout.reconfigure(encoding='utf-8')

DEVICE_ID = "56281FDCH002XD"

def get_phone_time():
    res = subprocess.run(f"adb -s {DEVICE_ID} shell date", shell=True, capture_output=True, text=True)
    out = res.stdout.strip()
    print(f"Phone date: {out}")
    m = re.search(r'(\d{2}):(\d{2}):(\d{2})', out)
    if m:
        return int(m.group(1)), int(m.group(2))
    return 14, 10

def setup_databases():
    # 1. Get phone time
    h, m = get_phone_time()
    # Set rule to 3 minutes ago (inside the 15-minute window)
    target_m = m - 3
    target_h = h
    if target_m < 0:
        target_m += 60
        target_h -= 1
        if target_h < 0:
            target_h += 24
            
    print(f"Target rule time calculated: {target_h:02d}:{target_m:02d}")
    
    # 2. Pull settings_database
    print("Pulling settings_database from phone...")
    subprocess.run(f'adb -s {DEVICE_ID} exec-out run-as com.heckmannch.birthdaybuddy cat databases/settings_database > settings_database_phone_temp', shell=True)
    
    db_path = "settings_database_phone_temp"
    if not os.path.exists(db_path) or os.path.getsize(db_path) < 100:
        print("Failed to pull database!")
        sys.exit(1)
        
    # 3. Update settings_database
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()
    try:
        cursor.execute("""
        CREATE TABLE IF NOT EXISTS notification_rules (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            daysBefore INTEGER NOT NULL,
            hour INTEGER NOT NULL,
            minute INTEGER NOT NULL
        );
        """)
        cursor.execute("""
        CREATE TABLE IF NOT EXISTS app_settings (
            id INTEGER PRIMARY KEY NOT NULL,
            notificationsEnabled INTEGER NOT NULL,
            persistentNotifications INTEGER NOT NULL,
            onboardingCompleted INTEGER NOT NULL,
            lastSyncTimestamp INTEGER NOT NULL,
            calendarSyncEnabled INTEGER NOT NULL,
            calendarId INTEGER
        );
        """)
        
        # Insert or replace notification rule at id = 1
        cursor.execute("INSERT OR REPLACE INTO notification_rules (id, daysBefore, hour, minute) VALUES (1, 0, ?, ?);", (target_h, target_m))
        
        # Insert or replace app settings at id = 0
        cursor.execute("""
        INSERT OR REPLACE INTO app_settings 
        (id, notificationsEnabled, persistentNotifications, onboardingCompleted, lastSyncTimestamp, calendarSyncEnabled, calendarId) 
        VALUES (0, 1, 1, 1, 0, 1, 17);
        """)
        
        conn.commit()
        print("Successfully updated database settings locally.")
    except Exception as e:
        print(f"Error modifying database: {e}")
        sys.exit(1)
    finally:
        conn.close()
        
    # 4. Clean up phone WAL and SHM journal files
    print("Cleaning up old journal files on the phone...")
    subprocess.run(f'adb -s {DEVICE_ID} shell "run-as com.heckmannch.birthdaybuddy rm databases/settings_database-wal 2>/dev/null || true"', shell=True)
    subprocess.run(f'adb -s {DEVICE_ID} shell "run-as com.heckmannch.birthdaybuddy rm databases/settings_database-shm 2>/dev/null || true"', shell=True)
    
    # 5. Push modified database back to the phone
    print("Pushing modified database to the phone...")
    subprocess.run(f'adb -s {DEVICE_ID} push settings_database_phone_temp /data/local/tmp/settings_database', shell=True)
    subprocess.run(f'adb -s {DEVICE_ID} shell "run-as com.heckmannch.birthdaybuddy cp /data/local/tmp/settings_database databases/settings_database"', shell=True)
    subprocess.run(f'adb -s {DEVICE_ID} shell "rm /data/local/tmp/settings_database 2>/dev/null || true"', shell=True)
    print("Push complete.")

def restart_app():
    print("Restarting the application on the phone...")
    subprocess.run(f"adb -s {DEVICE_ID} shell am force-stop com.heckmannch.birthdaybuddy", shell=True)
    time.sleep(1)
    subprocess.run(f"adb -s {DEVICE_ID} shell monkey -p com.heckmannch.birthdaybuddy -c android.intent.category.LAUNCHER 1", shell=True)
    print("App restarted. Waiting 6 seconds for contact sync...")
    time.sleep(6)

if __name__ == "__main__":
    setup_databases()
    restart_app()
    print("Onboarding bypass and sync complete! Next step is to find the Job ID and force it.")
