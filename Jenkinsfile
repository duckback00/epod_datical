pipeline {
  agent {
    node {
      label 'datical'
      customWorkspace "/var/lib/jenkins/workspace/epod_datical_master/1.Package-${BUILD_NUMBER}/"
    }
  }
 
  environment {
	GITURL="ssh://git@github.com:duckback00/"
	GIT_DATICAL_REPO="epod_datical"
	GIT_SQL_REPO="epod_sql"
	PROJ_DDB="epod_datical"
	PROJ_SQL="epod_sql"
    DATICAL_PIPELINE="${params.DATICAL_PIPELINE}"
	BRANCH="${params.BRANCH}"
    RELEASE_LABEL="${params.RELEASE_LABEL}"
	// REPOSITORY_BASE="MMB"
	ORACLE_HOME="/opt/oracle/product/12.1/client"
    PATH="$PATH:/opt/datical/DaticalDB/repl:$ORACLE_HOME/bin"
 
  }
  stages {

    stage ('Precheck') {
		steps {
			sh '''
				echo ORACLE_HOME=${ORACLE_HOME}
				echo PATH=${PATH}
				whoami
				which git
				#which expdp
				git --version
				git config --global user.email "jenkins@datical.com"
				git config --global user.name "jenkins"
			'''
		} // steps
	} // stage 'precheck'

    stage ('Checkout') {
      steps {
        deleteDir()
 
        // checkout Datical project from DDB repo
        checkout([
            $class: 'GitSCM',
            branches: [[name: '*/master']],
            doGenerateSubmoduleConfigurations: false,
            extensions: [
              [$class: 'RelativeTargetDirectory', relativeTargetDir: "${PROJ_DDB}"],
              [$class: 'LocalBranch', localBranch: 'master']],
            submoduleCfg: [],
            userRemoteConfigs: [[url: "${GITURL}/${GIT_DATICAL_REPO}.git"]]
        ])
  
        // checkout SQL scripts from SQL repo
        //         refspec: "+refs/heads/$BRANCH:refs/remotes/origin/$BRANCH" 
       checkout([
            $class: 'GitSCM',
            branches: [[name: "$BRANCH"]],
            doGenerateSubmoduleConfigurations: false,
            extensions: [
				[$class: 'RelativeTargetDirectory', relativeTargetDir: "${PROJ_SQL}"], 
				[$class: 'LocalBranch', localBranch: "${BRANCH}"]],
            submoduleCfg: [],
            userRemoteConfigs: [[url: "${GITURL}/${GIT_SQL_REPO}.git"]]
        ])

      } // steps for checkout stages
    } // stage 'checkout'
 
 
   stage ('Branches'){
      steps {
        sh '''
          #{ set +x; } 2>/dev/null
 
          cd ${PROJ_DDB}
          echo "Current Directory:" `pwd`
          git branch --set-upstream-to=origin/master master
          git status
 
          cd ../${PROJ_SQL}
          echo "Current Directory: " `pwd`
            git branch --set-upstream-to=origin/$BRANCH $BRANCH
 
          git status 
        '''
      } // steps
    }   // Branches stage
  
    stage('Packager') {
      steps {
 
        // get BitBucket username and password
  //       withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'BitbucketJenkins',
   //                                  usernameVariable: 'SQL_SCM_USER', passwordVariable: 'SQL_SCM_PASS']]) {
		//	withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'DDB_CREDENTIAL',
		//								usernameVariable: 'DDB_USER', passwordVariable: 'DDB_PASS']]) {
		//		withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'DDB_AUDIT_CREDENTIAL',
		//									usernameVariable: 'DDB_AUDIT_USER', passwordVariable: 'DDB_AUDIT_PASS']]) {

		 
					sh '''
					  { set +x; } 2>/dev/null
											  
					  cd ${PROJ_DDB}
					  echo
					  echo "==== Running - hammer version ===="
					  hammer show version
			 
					  # invoke Datical DB's Deployment Packager
					  echo "==== Running Deployment Packager ===="

					  hammer groovy deployPackager.groovy pipeline=${DATICAL_PIPELINE} scm=true labels="${BUILD_NUMBER},${RELEASE_LABEL}"

					  '''
	//			} // with Credentials (AuditDB)
	//		} // with Credentials (OracleDB)
  //      } // with Credentials (SCM)
      }   // steps
    }  // Packager step
 
  }   // stages
  post {
    always {
      // Jenkins Artifacts
      archiveArtifacts '**/daticaldb.log, **/Reports/**, **/Logs/**, **/Snapshots/** ,**/*.zip'
    }
  }
}     // pipeline
