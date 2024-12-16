System requirements
Install Android Studio SDK for modifying the Companion app.
Install : MariaDB 11.6.2 
Install Python v3.13.0:60403a5 (minimum)
Install in Commandline (windows):
    - pip install mariadb
    - pip install flask
    - pip install mysql
    - pip install mysql-connector-python
    - pip install qrcode[pil]
    - pip install pyzt
    - pip install werkzeug
    

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
    - Use browser: http://192.168.0.100:5000 (where as the ip should be your server's static ip)
    - Admin login: User: admin | password: Admin
    - Registration is straight forward
    - To rename website reffer to  app.py and index.html
    - Ensure new server and own ip adress modify app.py -> "#Connecting to DataBase server"
      under host = 0.0.0.0 (example the server IP is set to my IP, and keep the ''-markings)
    - New admin password and username: Modify app.py at :
      "@app.route('/admin_login', methods=['GET', 'POST'])" Under admin_username = 
      and andmin_password = (note keep the '' - markings )

Serverside Requirements within MariaDB:
    - for new users to be added who remote connects to the server in order to modify the app:
        GRANT ALL PRIVILEGES ON *.* TO 'username'@'%' IDENTIFIED BY 'password';
        FLUSH PRIVILEGES; (change username as you specify in app.py, and password 
        as you specify in app.py)

Android prototype app:
    - Requires to be downloaded, and installed 
    -Requires safety override as this app is prototype for now
    - Android 8.0 and newer versions minimum 
    -screen size 420dpi minimum
    -scrolling is not an option, landscape mode not supported correctly 
    - once the Qr-code started the Clock the app runs in background 
    - Manual logout must be done to update work logs 
    
    Within the Android Studio SDK: 
        - The following directories lead you: Ranger_team_project\Companionapp\app\src\main\java\com\example\bethonworkercompanion 
        - To find: the 2 specific files: "RetrofitClient.kt" and "RetryInterceptor.kt" 
        - locate the followinf finction: object RetrofitClient {
                                                            private const val PRIMARY_BASE_URL = "mod.ify.the.ip" within the caption
        to ensure the app communicates with the server once is set for static ip. 
        - The "RetryInterceptor.kt" can be deleted and the "RetrofitClient.kt"  needs only a modification for 
          the "RetryInterceptor.kt"  parts safely removed if this is troublesome  set the Ip within the file to the same IP as
          the "RetrofitClient.kt"  if that fails set it to something else, as the PRIMARY_BASE_URL is more than enough,
          for a normal server to be operating. 