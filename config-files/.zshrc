# Path to your oh-my-zsh installation.
  export ZSH=/home/mgumowski/.oh-my-zsh

# Set name of the theme to load.
# Look in ~/.oh-my-zsh/themes/
# Optionally, if you set this to "random", it'll load a random theme each
# time that oh-my-zsh is loaded.
ZSH_THEME="robbyrussell"

# Uncomment the following line to use case-sensitive completion.
# CASE_SENSITIVE="true"

# Uncomment the following line to use hyphen-insensitive completion. Case
# sensitive completion must be off. _ and - will be interchangeable.
# HYPHEN_INSENSITIVE="true"

# Uncomment the following line to disable bi-weekly auto-update checks.
# DISABLE_AUTO_UPDATE="true"

# Uncomment the following line to change how often to auto-update (in days).
# export UPDATE_ZSH_DAYS=13

# Uncomment the following line to disable colors in ls.
# DISABLE_LS_COLORS="true"

# Uncomment the following line to disable auto-setting terminal title.
# DISABLE_AUTO_TITLE="true"

# Uncomment the following line to enable command auto-correction.
# ENABLE_CORRECTION="true"

# Uncomment the following line to display red dots whilst waiting for completion.
# COMPLETION_WAITING_DOTS="true"

# Uncomment the following line if you want to disable marking untracked files
# under VCS as dirty. This makes repository status check for large repositories
# much, much faster.
# DISABLE_UNTRACKED_FILES_DIRTY="true"

# Uncomment the following line if you want to change the command execution time
# stamp shown in the history command output.
# The optional three formats: "mm/dd/yyyy"|"dd.mm.yyyy"|"yyyy-mm-dd"
# HIST_STAMPS="mm/dd/yyyy"

# Would you like to use another custom folder than $ZSH/custom?
# ZSH_CUSTOM=/path/to/new-custom-folder

# Which plugins would you like to load? (plugins can be found in ~/.oh-my-zsh/plugins/*)
# Custom plugins may be added to ~/.oh-my-zsh/custom/plugins/
# Example format: plugins=(rails git textmate ruby lighthouse)
# Add wisely, as too many plugins slow down shell startup.
plugins=(git mvn)

# User configuration

export PATH="/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/games:/usr/local/games"
# export MANPATH="/usr/local/man:$MANPATH"

source $ZSH/oh-my-zsh.sh

# You may need to manually set your language environment
# export LANG=en_US.UTF-8

# Preferred editor for local and remote sessions
# if [[ -n $SSH_CONNECTION ]]; then
#   export EDITOR='vim'
# else
#   export EDITOR='mvim'
# fi

# Compilation flags
# export ARCHFLAGS="-arch x86_64"

# ssh
# export SSH_KEY_PATH="~/.ssh/dsa_id"

# Set personal aliases, overriding those provided by oh-my-zsh libs,
# plugins, and themes. Aliases can be placed here, though oh-my-zsh
# users are encouraged to define aliases within the ZSH_CUSTOM folder.
# For a full list of active aliases, run `alias`.
#
# Example aliases
# alias zshconfig="mate ~/.zshrc"
# alias ohmyzsh="mate ~/.oh-my-zsh"

# --------------------------------- usefull aliases --------------------------------------------------------------------
alias cd..='cd ..'
alias ls='ls -F --color --show-control-chars -als'

# --------------------------------- maven related ----------------------------------------------------------------------
alias mvn="mvn-color" #requires mvn plugin for oh-my-zsh
alias mci='mvn clean install'
alias mciSkipTests='mvn clean install -DskipTests'
alias mciWithTests='mvn clean install -DskipTests=False'
alias mvnDebug='/usr/lib/mvn/apache-maven-3.2.5/bin/mvnDebug'

# --------------------------------- git related ------------------------------------------------------------------------
# go to git!
alias gut='git'
alias got='gut'
alias gti='got'

alias gtg='cd ~/wks/git/'
alias gtj="cd ~/wks/git/sonar-java/"
alias gte="cd ~/wks/eclipse-neon/"

alias gh='git hist'
alias gri='git rebase --interactive'
alias grih2='git rebase --interactive HEAD~2'
alias gpr='git pull --rebase'
alias gco='git checkout'
# git clean branches remotes
alias gcbr='git remote prune origin'
# git clean branches localy
alias gcbl='git branch --merged master | grep -v "\* master" | xargs -n 1 git branch -d'
# git rm for all deleted files
alias grmd='git ls-files --deleted -z | xargs -0 git rm'

# --------------------------------- work-related -----------------------------------------------------------------------
export SONARQUBE_HOME="$HOME/app/sonarqube-6.5.0.26101"
export SONARQUBE_LTS_HOME="$HOME/app/sonarqube-5.6.6"
export SONARQUBE_SCANNER_HOME="$HOME/app/sonar-scanner-2.8"
export SONARLINT_CLI="$HOME/app/sonarlint-cli-2.1.0.566"
export ORCHESTRATOR_CONFIG_URL="file:///home/mgumowski/wks/orchestrator.properties"
export RULE_API_VERSION="1.16"

export GRADLE_HOME="/usr/lib/gradle/3.5"

export PATH="$SONARQUBE_SCANNER_HOME/bin:$SONARLINT_CLI/bin:$GRADLE_HOME/bin:$PATH"

alias sq="$SONARQUBE_HOME/bin/linux-x86-64/./sonar.sh"
alias sqLTS="$SONARQUBE_LTS_HOME/bin/linux-x86-64/./sonar.sh"
alias sonarScanner='sonar-scanner'

sonarScannerDebug() {
  export SONAR_SCANNER_OPTS="-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000" && sonar-scanner
}

sonarLintDebug() {
  export SONARLINT_OPTS="-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000" && sonarlint
}

ruleDoc() {
  destPath=~/wks/git/sonar-java/java-checks/src/main/resources/org/sonar/l10n/java/rules/squid/
  java -jar ~/wks/git/sonar-rule-api/target/rule-api-"$RULE_API_VERSION"-SNAPSHOT.jar generate -language java -directory $destPath -rule $1 
}

ruleDocUpdate() {
  destPath=~/wks/git/sonar-java/java-checks/src/main/resources/org/sonar/l10n/java/rules/squid/
  java -jar ~/wks/git/sonar-rule-api/target/rule-api-"$RULE_API_VERSION"-SNAPSHOT.jar update -language java -directory $destPath
}

ruling(){
  java -version
  mci
  cd its/ruling
  mciWithTests
  cd ../..
}

rulingNoPlugin(){
  java -version
  cd its/ruling
  mciWithTests
  cd ../..
}

copyRulingJson(){
for myDir in ~/Development/SonarSource/sonar-java/its/ruling/target/actual/*
do
  if [ -d "$myDir" ]; then
    if [ ! -f $myDir/squid-"$*".json ]; then
      echo "File not found: all issues of project have been removed: "
      rm ~/Development/SonarSource/sonar-java/its/ruling/src/test/resources/$(basename "$myDir")/squid-"$*".json
    else
      cp $myDir/squid-"$*".json ~/Development/SonarSource/sonar-java/its/ruling/src/test/resources/$(basename "$myDir") || echo "Cannot copy for $myDir"
    fi
  fi
done
}

# --------------------------------- jenv ; SHOULD BE LAST --------------------------------------------------------------
export PATH="$HOME/.jenv/bin:$PATH"
eval "$(jenv init -)"

# added by travis gem
[ -f /home/mgumowski/.travis/travis.sh ] && source /home/mgumowski/.travis/travis.sh
