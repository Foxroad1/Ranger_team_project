- Time tracking app for workers with base requirements: 
    Multi-user application
    Uses REST API
    Employees log hours into the system under a specific category
    Summary on the website

RQ-001-Customer: App should handle multiple users without a mistake,
without possible, "hicups". 
    RQ-001b-Customer: No limitations on upper limit by numbers of users at the moment.

RQ-002-Customer: The app uses REST API. 

RQ-003-Customer: Employee ID numbers generated automatically,
an account is created for an employee, with their name, date of birth, title,
start date of employment contract and ID number.
    RQ-003b-Customer: Employee details needs to be added manually, The logins uses Worker Username.

RQ-004-Customer: Employee work hours logged automatically based on Logging in and out. 
    RQ-004b-Customer: Allows to sign in to shift with QRcode readings. (optionally) 
        RQ-004c-Customer: The app creates a summary on the website, weekly / monthly work hours completed,
        and on what tittle if multiple.

RQ-005-Customer:  The first version will include hours entry,
by logging into the application and manually confirming the start time, date and the end of work shift.

RQ-006-Customer:  Employee information will be compiled on a website, where you can check more detailed information.

 


RQ-001-DEV: Team splits up roles (COMPLETED)
    RQ-001b-DEV: At all Team meetings team revises ideas, and roles.

RQ-002-DEV: Setting up a server, for database handling.(COMPLETED)
    RQ-002b-DEV: Creating a website for the server as a User Interface. (Admin side and employee side?) COMPLETED

RQ-003-DEV: Creating the App User Interface (UI). COMPLETED

RQ-004-DEV: Developing Registration methods (Web side COMPLETED all)
    RQ-004b-DEV: ensuring Employee ID creation after registration.
    RQ-004c-DEV: stores and updates new Employee, and new information about Employee.

RQ-005-DEV: Developing the communication logic between prototype (app) and server. COMPLETED

RQ-006-DEV: Developing the prototype (app)  to be able to login fetch data for profile. COMPLETED

RQ-007-DEV: Generating a QRcode that is hardcoded to location. COMPLETED

RQ-008-DEV: Clock starts with QRcode being scanned and the phone location matches the QRcode hard coded location. COMPLETED

RQ-009-DEV: Upon Logout the app updates the server with the measured time and save them into logs. COMPLETED

RQ-010-DEV-Extra-Feature: The web side users, and admins can download ".csv" file formatted summarized logs 

(More to be added as DEV team proceeds) 