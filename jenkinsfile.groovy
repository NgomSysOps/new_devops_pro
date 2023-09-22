pipeline{
    agent {label 'Nodes1'}
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
        
        stage('Trivy Scan Config files'){
            steps{
                sh 'trivy fs --severity HIGH,CRITICAL --scanners config /home/ngom/jenkins_root_directory/workspace/testpipeline'
            }
        }
        
        stage('Building docker image'){
            steps{
                sh 'docker build -t ngomansible/my_private_repo:latest .'
            }
        }
        
        stage('Trivy Scan Docker image'){
            steps{
                sh 'trivy image --severity HIGH,CRITICAL --no-progress --exit-code 0 ngomansible/my_private_repo:latest'
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
        
        stage('Deploy app to Kubernetes'){
            steps{
               withKubeConfig(caCertificate: '', clusterName: '', contextName: '', credentialsId: 'k8s-cred', namespace: '', restrictKubeConfigAccess: false, serverUrl: '') {
                   sh ' kubectl apply -f myDeployment.yml'
                }
            }
        }
    }
}