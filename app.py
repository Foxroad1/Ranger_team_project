import os
import csv
from datetime import datetime, timedelta
from flask import Flask, request, jsonify, render_template, redirect, url_for, session, send_file
import mariadb
import uuid
from werkzeug.security import generate_password_hash, check_password_hash
import jwt
from functools import wraps
import pytz

app = Flask(__name__)
app.secret_key = 'your_secret_key'
SECRET_KEY = 'your_jwt_secret_key'
FINLAND_TZ = pytz.timezone('Europe/Helsinki')

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
                host="192.168.0.100",  # Fallback to home IP
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

# Shared authentication logic
def authenticate_user(username, password):
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("SELECT * FROM employees WHERE username = ?", (username,))
    user = cur.fetchone()
    cur.close()
    conn.close()
    if user and check_password_hash(user[7], password):
        return user
    return None

# Web page login route
@app.route('/web_login', methods=['GET', 'POST'])
def web_login():
    if request.method == 'POST':
        username = request.form['username']
        password = request.form['password']
        user = authenticate_user(username, password)
        if user:
            token = jwt.encode({
                'user_id': user[0],
                'exp': datetime.now(pytz.UTC) + timedelta(hours=24)  # Use timezone-aware datetime
            }, app.secret_key, algorithm='HS256')
            session['token'] = token  # Store the token in the session
            return redirect(url_for('profile'))
        else:
            return render_template('web_login.html', error="Invalid username or password")
    return render_template('web_login.html')

# Token required decorator
def token_required(f):
    @wraps(f)
    def decorated(*args, **kwargs):
        token = request.headers.get('Authorization')
        if not token:
            token = session.get('token')  # Retrieve token from session if not in headers
        if not token:
            return jsonify({'message': 'Token is missing!'}), 401
        try:
            if isinstance(token, str) and ' ' in token:
                token = token.split(" ")[1]  # Extract token part after 'Bearer'
            data = jwt.decode(token, SECRET_KEY, algorithms=['HS256'])
            current_user = data['user_id']
        except jwt.ExpiredSignatureError:
            return jsonify({'message': 'Token has expired!'}), 401
        except jwt.InvalidTokenError:
            return jsonify({'message': 'Token is invalid!'}), 401
        return f(current_user, *args, **kwargs)
    return decorated

# Android login route
@app.route('/api/login', methods=['POST'])
def api_login():
    if request.method == 'POST':
        data = request.get_json()
        username = data['username']
        password = data['password']
        user = authenticate_user(username, password)
        if user:
            token = jwt.encode({
                'user_id': user[0],
                'exp': datetime.now(pytz.UTC) + timedelta(hours=24)  # Use timezone-aware datetime
            }, SECRET_KEY, algorithm='HS256')
            return jsonify({"success": True, "token": token})
        else:
            return jsonify({"success": False, "message": "Invalid username or password"}), 401

@app.route('/logout', methods=['POST'])
def logout():
    if 'user_id' in session:
        # Log the logout time
        conn = get_db_connection()
        cur = conn.cursor()
        cur.execute(
            "UPDATE work_logs SET log_out_time = ? WHERE employee_id = ? AND log_out_time IS NULL",
            (datetime.now(FINLAND_TZ), session['user_id'])
        )
        conn.commit()
        cur.close()
        conn.close()
        session.pop('user_id', None)
        session.pop('token', None)  # Remove token from session
    return redirect(url_for('web_login'))

@app.route('/profile')
@token_required
def profile(current_user):
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("SELECT * FROM employees WHERE id = ?", (current_user,))
    user = cur.fetchone()

    cur.execute("SELECT * FROM work_logs WHERE employee_id = ?", (current_user,))
    work_logs = cur.fetchall()

    # Summarize work hours by day, week, and month
    daily_summary = {}
    weekly_summary = {}
    monthly_summary = {}

    for log in work_logs:
        log_in_time = log[2].replace(tzinfo=pytz.UTC).astimezone(FINLAND_TZ)
        log_out_time = log[3].replace(tzinfo=pytz.UTC).astimezone(FINLAND_TZ) if log[3] else datetime.now(FINLAND_TZ)  # Use current time if still logged in
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

    user_data = {
        'id': user[0],
        'name': user[1],
        'dob': user[2],
        'title': user[3],
        'start_date': user[4],
        'worker_id': user[5],
        'username': user[6],
        'email': user[8],
        'phone_number': user[9]
    }

    work_logs_data = [
        {
            'log_in_time': log[2].replace(tzinfo=pytz.UTC).astimezone(FINLAND_TZ),
            'log_out_time': log[3].replace(tzinfo=pytz.UTC).astimezone(FINLAND_TZ) if log[3] else None,
            'title': log[4]
        } for log in work_logs
    ]

    if request.is_json:
        return jsonify({'user': user_data, 'work_logs': work_logs_data})
    else:
        return render_template('profile.html', user=user, work_logs=work_logs,
                               daily_summary=daily_summary,
                               weekly_summary=weekly_summary,
                               monthly_summary=monthly_summary)

# Function to save work logs to a CSV file with calculated work hours in hours, minutes, and seconds format
def save_work_logs_to_csv(logs, filename):
    with open(filename, 'w', newline='') as csvfile:
        fieldnames = ['Employee ID', 'Log In Time', 'Log Out Time', 'Title', 'Work Hours']
        writer = csv.DictWriter(csvfile, fieldnames=fieldnames)

        writer.writeheader()
        for log in logs:
            log_in_time = log[2].replace(tzinfo=pytz.UTC).astimezone(FINLAND_TZ)
            log_out_time = log[3].replace(tzinfo.pytz.UTC).astimezone(FINLAND_TZ) if log[3] else datetime.now(FINLAND_TZ)  # Use current time if still logged in
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
        date_str = datetime.now(FINLAND_TZ).strftime('%Y-%m-%d')
        cur.execute("SELECT * FROM work_logs WHERE DATE(log_in_time) = %s", (date_str,))
        filename = f'work_logs/work_logs_daily_{date_str}.csv'
    elif period == 'weekly':
        week_str = datetime.now(FINLAND_TZ).strftime('%Y-%U')
        cur.execute("SELECT * FROM work_logs WHERE YEARWEEK(log_in_time, 1) = YEARWEEK(%s, 1)", (datetime.now(FINLAND_TZ),))
        filename = f'work_logs/work_logs_weekly_{week_str}.csv'
    elif period == 'monthly':
        month_str = datetime.now(FINLAND_TZ).strftime('%Y-%m')
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
        date_str = datetime.now(FINLAND_TZ).strftime('%Y-%m-%d')
        cur.execute("SELECT * FROM work_logs WHERE employee_id = ? AND DATE(log_in_time) = ?", (employee_id, date_str))
        filename = f'work_logs/work_logs_daily_{employee_id}_{date_str}.csv'
    elif period == 'weekly':
        week_str = datetime.now(FINLAND_TZ).strftime('%Y-%U')
        cur.execute("SELECT * FROM work_logs WHERE employee_id = ? AND YEARWEEK(log_in_time, 1) = YEARWEEK(?, 1)",
                    (employee_id, datetime.now(FINLAND_TZ)))
        filename = f'work_logs/work_logs_weekly_{employee_id}_{week_str}.csv'
    elif period == 'monthly':
        month_str = datetime.now(FINLAND_TZ).strftime('%Y-%m')
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