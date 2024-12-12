import os
import csv
from datetime import datetime, timedelta
from flask import Flask, request, jsonify, render_template, redirect, url_for, session, send_file
import mariadb
import uuid
from werkzeug.security import generate_password_hash, check_password_hash

app = Flask(__name__)
app.secret_key = 'your_secret_key'

def get_db_connection():
    try:
        conn = mariadb.connect(
            user="root",
            password="1542",
            host="10.6.128.19",  # Use the school ip
            port=3306,
            database="bethon_worker"
        )
        return conn
    except mariadb.OperationalError:
        try:
            conn = mariadb.connect(
                user="root",
                password="1542",
                host="88.115.201.36",  # Fallback to home IP
                port=3306,
                database="bethon_worker"
            )
            return conn
        except mariadb.OperationalError as e:
            print(f"Error connecting to MariaDB: {e}")
            return None

@app.route('/test_db')
def test_db():
    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        cursor.execute('SELECT 1')
        result = cursor.fetchone()
        conn.close()
        return 'Database connection successful!' if result else 'No result!'
    except Exception as e:
        return f'Error: {e}'


@app.route('/')
def index():
    return render_template('index.html')


@app.route('/register', methods=['GET', 'POST'])
def register():
    if request.method == 'POST':
        data = request.form
        worker_id = str(uuid.uuid4())
        hashed_password = generate_password_hash(data['password'], method='pbkdf2:sha256')
        conn = get_db_connection()
        cur = conn.cursor()

        # Find the lowest available ID
        cur.execute("SELECT id FROM employees ORDER BY id")
        ids = [row[0] for row in cur.fetchall()]
        new_id = 1
        for i in range(len(ids)):
            if ids[i] != i + 1:
                new_id = i + 1
                break
            new_id = len(ids) + 1

        cur.execute(
            "INSERT INTO employees (id, name, dob, title, start_date, worker_id, username, password, email, phone_number) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            (new_id, data['name'], data['dob'], data['title'], data['start_date'], worker_id, data['username'],
             hashed_password,
             data['email'], data['phone_number'])
        )
        conn.commit()
        cur.close()
        conn.close()
        return redirect(url_for('login'))
    return render_template('register.html')


@app.route('/login', methods=['GET', 'POST'])
def login():
    if request.method == 'POST':
        data = request.form
        conn = get_db_connection()
        cur = conn.cursor()
        cur.execute("SELECT * FROM employees WHERE username = ?", (data['username'],))
        user = cur.fetchone()
        cur.close()
        conn.close()
        if user and check_password_hash(user[7], data['password']):
            session['user_id'] = user[0]
            # Check for an existing session
            conn = get_db_connection()
            cur = conn.cursor()
            cur.execute("SELECT * FROM work_logs WHERE employee_id = ? AND log_out_time IS NULL", (session['user_id'],))
            existing_session = cur.fetchone()
            if not existing_session:
                # Log the login time
                cur.execute(
                    "INSERT INTO work_logs (employee_id, log_in_time) VALUES (?, ?)",
                    (session['user_id'], datetime.now())
                )
                conn.commit()
            cur.close()
            conn.close()
            return jsonify({"success": True, "userId": user[0]})
        return jsonify({"success": False, "message": "Invalid username or password"}), 401
    return render_template('login.html')


@app.route('/logout', methods=['POST'])
def logout():
    if 'user_id' in session:
        # Log the logout time
        conn = get_db_connection()
        cur = conn.cursor()
        cur.execute(
            "UPDATE work_logs SET log_out_time = ? WHERE employee_id = ? AND log_out_time IS NULL",
            (datetime.now(), session['user_id'])
        )
        conn.commit()
        cur.close()
        conn.close()
        session.pop('user_id', None)
    return redirect(url_for('login'))


@app.route('/profile')
def profile():
    if 'user_id' not in session:
        return redirect(url_for('login'))
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("SELECT * FROM employees WHERE id = ?", (session['user_id'],))
    user = cur.fetchone()

    # Fetch work logs for the logged-in user
    cur.execute("SELECT * FROM work_logs WHERE employee_id = ?", (session['user_id'],))
    work_logs = cur.fetchall()

    # Summarize work hours by day, week, and month
    daily_summary = {}
    weekly_summary = {}
    monthly_summary = {}

    for log in work_logs:
        log_in_time = log[2]
        log_out_time = log[3] if log[3] else datetime.now()  # Use current time if still logged in
        date_str = log_in_time.strftime('%Y-%m-%d')

        # Calculate the duration of each work session
        duration = log_out_time - log_in_time

        # Daily summary
        if date_str not in daily_summary:
            daily_summary[date_str] = timedelta()
        daily_summary[date_str] += duration

        # Weekly summary
        week_str = log_in_time.strftime('%Y-%U')
        if week_str not in weekly_summary:
            weekly_summary[week_str] = timedelta()
        weekly_summary[week_str] += duration

        # Monthly summary
        month_str = log_in_time.strftime('%Y-%m')
        if month_str not in monthly_summary:
            monthly_summary[month_str] = timedelta()
        monthly_summary[month_str] += duration

    cur.close()
    conn.close()

    if request.is_json:
        return jsonify({
            "user": user,
            "work_logs": work_logs,
            "daily_summary": daily_summary,
            "weekly_summary": weekly_summary,
            "monthly_summary": monthly_summary
        })
    else:
        return render_template('profile.html', user=user, work_logs=work_logs,
                               daily_summary=daily_summary,
                               weekly_summary=weekly_summary,
                               monthly_summary=monthly_summary)


@app.route('/log_work', methods=['GET', 'POST'])
def log_work():
    if 'user_id' not in session:
        return redirect(url_for('login'))
    if request.method == 'POST':
        data = request.form
        conn = get_db_connection()
        cur = conn.cursor()
        cur.execute(
            "INSERT INTO work_logs (employee_id, log_in_time, log_out_time, Title) VALUES (?, ?, ?, ?)",
            (session['user_id'], data['log_in_time'], data['log_out_time'], data['category'])
        )
        conn.commit()
        cur.close()
        conn.close()
        return redirect(url_for('profile'))
    return render_template('log_work.html')

@app.route('/log_start_time', methods=['POST'])
def log_start_time():
    if 'user_id' not in session:
        return jsonify({'error': 'Unauthorized'}), 401

    data = request.get_json()
    qr_code = data.get('qrCode')

    if not qr_code:
        return jsonify({'error': 'QR code is required'}), 400

    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute(
        "INSERT INTO work_logs (employee_id, log_in_time) VALUES (?, ?)",
        (session['user_id'], datetime.now())
    )
    conn.commit()
    cur.close()
    conn.close()

    return jsonify({'message': 'Start time logged successfully'}), 200

@app.route('/log_end_time', methods=['POST'])
def log_end_time():
    if 'user_id' not in session:
        return jsonify({'error': 'Unauthorized'}), 401

    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute(
        "UPDATE work_logs SET log_out_time = ? WHERE employee_id = ? AND log_out_time IS NULL",
        (datetime.now(), session['user_id'])
    )
    conn.commit()
    cur.close()
    conn.close()

    return jsonify({'message': 'End time logged successfully'}), 200


@app.route('/admin', methods=['GET', 'POST'])
def admin():
    if 'admin_logged_in' not in session:
        return redirect(url_for('admin_login'))

    if request.method == 'POST':
        # Add employee to the database
        if 'add_employee' in request.form:
            data = request.form
            worker_id = str(uuid.uuid4())
            hashed_password = generate_password_hash(data['password'], method='pbkdf2:sha256')
            conn = get_db_connection()
            cur = conn.cursor()

            # Find the lowest available ID
            cur.execute("SELECT id FROM employees ORDER BY id")
            ids = [row[0] for row in cur.fetchall()]
            new_id = 1
            for i in range(len(ids)):
                if ids[i] != i + 1:
                    new_id = i + 1
                    break
                new_id = len(ids) + 1

            cur.execute(
                "INSERT INTO employees (id, name, dob, title, start_date, worker_id, username, password, email, phone_number) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                (new_id, data['name'], data['dob'], data['title'], data['start_date'], worker_id, data['username'],
                 hashed_password, data['email'], data['phone_number'])
            )
            conn.commit()
            cur.close()
            conn.close()

        # Remove employee from the database
        elif 'remove_employee' in request.form:
            employee_id = request.form.get('employee_id')
            conn = get_db_connection()
            cur = conn.cursor()
            # Delete related work logs first
            cur.execute("DELETE FROM work_logs WHERE employee_id=%s", (employee_id,))
            cur.execute("DELETE FROM employees WHERE id=%s", (employee_id,))
            conn.commit()
            cur.close()
            conn.close()

        # Modify employee details
        elif 'modify_employee' in request.form:
            employee_id = request.form.get('employee_id')
            title = request.form.get('title')
            work_group = request.form.get('work_group')
            conn = get_db_connection()
            cur = conn.cursor()
            cur.execute("UPDATE employees SET title=%s WHERE id=%s", (title, employee_id))
            conn.commit()
            cur.close()
            conn.close()

    # Fetch all employees and work logs for display
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("SELECT * FROM employees")
    employees = cur.fetchall()
    cur.execute("SELECT * FROM work_logs")
    work_logs = cur.fetchall()
    cur.close()
    conn.close()

    return render_template('admin.html', employees=employees, work_logs=work_logs)


@app.route('/admin_login', methods=['GET', 'POST'])
def admin_login():
    if request.method == 'POST':
        data = request.form
        admin_username = "admin"  # Replace with your admin username
        admin_password = "Admin"  # Replace with your admin password
        if data['username'] == admin_username and data['password'] == admin_password:
            session['admin_logged_in'] = True
            return redirect(url_for('admin'))
        return 'Invalid username or password'
    return render_template('admin_login.html')


@app.route('/admin_logout', methods=['POST'])
def admin_logout():
    session.pop('admin_logged_in', None)
    return redirect(url_for('admin_login'))


# Function to save work logs to a CSV file with calculated work hours in hours, minutes, and seconds format
def save_work_logs_to_csv(logs, filename):
    with open(filename, 'w', newline='') as csvfile:
        fieldnames = ['Employee ID', 'Log In Time', 'Log Out Time', 'Title', 'Work Hours']
        writer = csv.DictWriter(csvfile, fieldnames=fieldnames)

        writer.writeheader()
        for log in logs:
            log_in_time = log[2]
            log_out_time = log[3] if log[3] else datetime.now()  # Use current time if still logged in
            duration = log_out_time - log_in_time
            hours, remainder = divmod(duration.total_seconds(), 3600)
            minutes, seconds = divmod(remainder, 60)
            work_hours = f'{int(hours)}h {int(minutes)}m {int(seconds)}s'  # Convert duration to HH:MM:SS
            writer.writerow({
                'Employee ID': log[1],
                'Log In Time': log_in_time,
                'Log Out Time': log_out_time,
                'Title': log[4],
                'Work Hours': work_hours
            })


# Function to generate and save work logs for a specific period
def generate_work_logs(period):
    conn = get_db_connection()
    cur = conn.cursor()

    if period == 'daily':
        date_str = datetime.now().strftime('%Y-%m-%d')
        cur.execute("SELECT * FROM work_logs WHERE DATE(log_in_time) = %s", (date_str,))
        filename = f'work_logs/work_logs_daily_{date_str}.csv'
    elif period == 'weekly':
        week_str = datetime.now().strftime('%Y-%U')
        cur.execute("SELECT * FROM work_logs WHERE YEARWEEK(log_in_time, 1) = YEARWEEK(%s, 1)", (datetime.now(),))
        filename = f'work_logs/work_logs_weekly_{week_str}.csv'
    elif period == 'monthly':
        month_str = datetime.now().strftime('%Y-%m')
        cur.execute("SELECT * FROM work_logs WHERE DATE_FORMAT(log_in_time, '%Y-%m') = %s", (month_str,))
        filename = f'work_logs/work_logs_monthly_{month_str}.csv'

    logs = cur.fetchall()
    cur.close()
    conn.close()

    save_work_logs_to_csv(logs, filename)
    return filename


# Function to generate and save work logs for a specific employee and period
def generate_employee_work_logs(employee_id, period):
    conn = get_db_connection()
    cur = conn.cursor()

    if period == 'daily':
        date_str = datetime.now().strftime('%Y-%m-%d')
        cur.execute("SELECT * FROM work_logs WHERE employee_id = ? AND DATE(log_in_time) = ?", (employee_id, date_str))
        filename = f'work_logs/work_logs_daily_{employee_id}_{date_str}.csv'
    elif period == 'weekly':
        week_str = datetime.now().strftime('%Y-%U')
        cur.execute("SELECT * FROM work_logs WHERE employee_id = ? AND YEARWEEK(log_in_time, 1) = YEARWEEK(?, 1)",
                    (employee_id, datetime.now()))
        filename = f'work_logs/work_logs_weekly_{employee_id}_{week_str}.csv'
    elif period == 'monthly':
        month_str = datetime.now().strftime('%Y-%m')
        cur.execute("SELECT * FROM work_logs WHERE employee_id = ? AND DATE_FORMAT(log_in_time, '%Y-%m') = ?",
                    (employee_id, month_str))
        filename = f'work_logs/work_logs_monthly_{employee_id}_{month_str}.csv'

    logs = cur.fetchall()
    cur.close()
    conn.close()

    save_work_logs_to_csv(logs, filename)
    return filename


# Route to download work logs for a specific employee and period
@app.route('/download_employee_work_logs/<employee_id>/<period>')
def download_employee_work_logs(employee_id, period):
    if period not in ['daily', 'weekly', 'monthly']:
        return "Invalid period specified", 400

    filename = generate_employee_work_logs(employee_id, period)

    return send_file(filename, as_attachment=True)


# Ensure the directory for storing CSV files exists
if not os.path.exists('work_logs'):
    os.makedirs('work_logs')

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)