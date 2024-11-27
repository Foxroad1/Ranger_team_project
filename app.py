from flask import Flask, request, jsonify, render_template, redirect, url_for, session
import mariadb
import uuid
from werkzeug.security import generate_password_hash, check_password_hash

app = Flask(__name__)
app.secret_key = 'your_secret_key'

# Database connection
def get_db_connection():
    conn = mariadb.connect(
        user="root",
        password="1542",
        host="localhost",
        port=3306,
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
            (data['name'], data['dob'], data['title'], data['start_date'], worker_id, data['username'], hashed_password, data['email'], data['social_security_number'])
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
            return redirect(url_for('profile'))
        return 'Invalid username or password'
    return render_template('login.html')

@app.route('/profile')
def profile():
    if 'user_id' not in session:
        return redirect(url_for('login'))
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("SELECT * FROM employees WHERE id = ?", (session['user_id'],))
    user = cur.fetchone()
    cur.execute("SELECT * FROM work_logs WHERE employee_id = ?", (session['user_id'],))
    work_logs = cur.fetchall()
    cur.close()
    conn.close()
    return render_template('profile.html', user=user, work_logs=work_logs)

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

if __name__ == '__main__':
    app.run(debug=True)