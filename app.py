from flask import Flask, request, jsonify, render_template, redirect, url_for, session
import mariadb
import uuid
from werkzeug.security import generate_password_hash, check_password_hash
from datetime import datetime, timedelta

app = Flask(__name__)
app.secret_key = 'your_secret_key'


# Database connection
def get_db_connection():
    conn = mariadb.connect(
        user="root",
        password="1542",
        host="localhost",
        database="bethon_worker"
    )
    return conn


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
        cur.execute(
            "INSERT INTO employees (name, dob, title, start_date, worker_id, username, password, email, social_security_number) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            (data['name'], data['dob'], data['title'], data['start_date'], worker_id, data['username'], hashed_password,
             data['email'], data['social_security_number'])
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
            # Log the login time
            conn = get_db_connection()
            cur = conn.cursor()
            cur.execute(
                "INSERT INTO work_logs (employee_id, log_in_time) VALUES (?, ?)",
                (session['user_id'], datetime.now())
            )
            conn.commit()
            cur.close()
            conn.close()
            return redirect(url_for('profile'))
        return 'Invalid username or password'
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
    cur.execute("SELECT * FROM employees WHERE id = %s", (session['user_id'],))
    user = cur.fetchone()

    # Fetch work logs for the logged-in user
    cur.execute("SELECT * FROM work_logs WHERE employee_id = %s", (session['user_id'],))
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
            "INSERT INTO work_logs (employee_id, log_in_time, log_out_time, category) VALUES (?, ?, ?, ?)",
            (session['user_id'], data['log_in_time'], data['log_out_time'], data['category'])
        )
        conn.commit()
        cur.close()
        conn.close()
        return redirect(url_for('profile'))
    return render_template('log_work.html')


@app.route('/admin', methods=['GET', 'POST'])
def admin():
    if request.method == 'POST':
        data = request.form
        worker_id = str(uuid.uuid4())
        hashed_password = generate_password_hash(data['password'], method='pbkdf2:sha256')
        conn = get_db_connection()
        cur = conn.cursor()
        cur.execute(
            "INSERT INTO employees (name, dob, title, start_date, worker_id, username, password, email, social_security_number) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)",
            (data['name'], data['dob'], data['title'], data['start_date'], worker_id, data['username'], hashed_password, data['email'], data['social_security_number'])
        )
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
        admin_password = "admin_password"  # Replace with your admin password
        if data['username'] == admin_username and data['password'] == admin_password:
            session['admin_logged_in'] = True
            return redirect(url_for('admin'))
        return 'Invalid username or password'
    return render_template('admin_login.html')


@app.route('/admin_logout')
def admin_logout():
    session.pop('admin_logged_in', None)
    return redirect(url_for('admin_login'))

if __name__ == '__main__':
    app.run(debug=True)