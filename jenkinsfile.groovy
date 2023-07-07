pipeline{
    agent {label 'label_name'}
    tools {
        maven "Maven3"
    }
    
    stages{
        
        stage('Checkout code from git'){
            steps{
                checkout scmGit(branches: [[name: '*/main']], extensions: [], userRemoteConfigs: [[url: 'https://github.com/NgomSysOps/ngom_k8s_project']])
            }
        }
        
        stage('Building Maven project'){
            steps{
                sh 'mvn clean install'
            }
        }
        
        stage('Scan code with SonarQube'){
            steps{
                withSonarQubeEnv('SonarQube') {
                    sh 'mvn sonar:sonar'
                }
            }
        }
        
        stage('Building docker image'){
            steps{
                sh 'docker build -t ngomansible/my_private_repo:latest .'
            }
        }
        
        stage('Push image to DockerHub'){
            steps{
                withCredentials([string(credentialsId: 'DockerToken', variable: 'docker_cred')]) {
                   sh 'docker login -u ngomansible -p ${docker_cred}' 
                }
                sh 'docker push ngomansible/my_private_repo:latest'
            }
        }

        stage('Deploy App to kubernetes'){
            steps{
                script{
                    kubernetesDeploy (configs: 'deployment.yaml',kubeconfigId: 'k8sConfig')
                }
            }
        }

    }
}