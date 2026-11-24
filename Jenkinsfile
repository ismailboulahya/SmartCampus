pipeline {
    agent any

    tools {
        maven 'Maven-3.9'
        nodejs 'NodeJS-22'
    }

    options {
        skipDefaultCheckout(true)
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Verify Environment') {
            steps {
                sh 'java -version'
                sh 'mvn -version'
                sh 'node -v'
                sh 'npm -v'
                sh 'ls -ld frontend'
            }
        }

        stage('Build') {
            steps {
                sh 'mvn -B clean package -DskipTests'
            }
        }

        stage('Test') {
            steps {
                sh 'mvn -B test'
            }
        }
    }
}