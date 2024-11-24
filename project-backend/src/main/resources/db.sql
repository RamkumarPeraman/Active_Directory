CREATE DATABASE userdb;
USE userdb;

CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100),
    email VARCHAR(100),
    phone VARCHAR(15)
);

INSERT INTO users (name, email, phone) VALUES
('Ram', 'ram@gmail.com', '9876543210'),
('Madhu', 'madhu@gmail.com', '9876543211'),
('Dinesh', 'dinesh@gmail.com', '9876543212'),
('Arun', 'arun@gmail.com', '9876543213');
