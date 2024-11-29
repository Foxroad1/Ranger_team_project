System requirements
Install : MariaDB 11.6.2 
Install Python v3.13.0:60403a5 (minimum)
Install in Commandline (windows):
    - pip install mariadb
    - pip install flask
    - pip install mysql
    - pip install mysql-connector-python
    

In Windows press Windows button (flag) 
    - Type in environment : -> click on "Edit the System Environment Variables"
    - select: Environment Variables: -> select PATH and click Edit
    - New : C:\Program Files\Python313 -> Enter
    - New: C:\Program Files\Python313\Scripts ->  Enter
    - New: C:\Program Files\MariaDB 11.6\bin -> Enter
Followed by clicking ok on al 3 window. 

Use CMD to initialize server: 
    - mysql -h 88.115.201.36 -P 3306 -u root -p
    - password: 1542
    - USE bethon_worker;

Use CMD to launch python app by:
    - cd C:\Ranger_team_project (or path where yoy store the main folder)
    - type in : python app.py
    - Use browser: http://127.0.0.1:5000
    - Admin login: User: admin | password: Admin
    - Registration is straight forward
    - To rename website reffer to  app.py and index.html
    - Ensure new server and own ip adress modify app.py -> "#Connecting to DataBase server"
      under host = 0.0.0.0 (example the server IP is set to my IP, and keep the ''-markings)
    - New admin password and username: Modify app.py at :
      "@app.route('/admin_login', methods=['GET', 'POST'])" Under admin_username = 
      and andmin_password = (note keep the '' - markings )

Serverside Requirements within MariaDB:
    - for other users :
        GRANT ALL PRIVILEGES ON *.* TO 'username'@'%' IDENTIFIED BY 'password';
        FLUSH PRIVILEGES; (change username as you specify in app.py, and password 
        as you specify in app.py)