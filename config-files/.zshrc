# Path to your oh-my-zsh installation.fullyQualifiedName
export ZSH=/home/michaelgumowski/.oh-my-zsh

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
plugins=(git gitfast mvn)

# User configuration

export PATH="/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/games:/usr/local/games:$PATH"
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
# export SSH_KEY_PATH="~/.ssh/github_rsa"

# Set personal aliases, overriding those provided by oh-my-zsh libs,
# plugins, and themes. Aliases can be placed here, though oh-my-zsh
# users are encouraged to define aliases within the ZSH_CUSTOM folder.
# For a full list of active aliases, run `alias`.
#
# Example aliases
# alias zshconfig="mate ~/.zshrc"
# alias ohmyzsh="mate ~/.oh-my-zsh"

# --------------------------------- ssh configuration ------------------------------------------------------------------
eval "$(ssh-agent -s)"
ssh-add ~/.ssh/github_rsa

# --------------------------------- usefull aliases --------------------------------------------------------------------
alias cd..='cd ..'
alias ls='ls -F --color --show-control-chars -als'

# --------------------------------- maven related ----------------------------------------------------------------------
export MAVEN_HOME="/usr/lib/mvn/apche-maven-3.8.1"
alias mci='mvn clean install'
alias mciSkipTests='mci -DskipTests'
alias mciWithTests='mci -DskipTests=False'

# --------------------------------- scanner for msbuild related---------------------------------------------------------
export SCANNER_FOR_MSBUILD="/home/michaelgumowski/sonarscanner-msbuild"

# --------------------------------- git related ------------------------------------------------------------------------
export GITHUB_TOKEN='...'

alias gut='git'
alias got='gut'
alias gti='got'

# go to git!
alias gtg='cd /home/michaelgumowski/dev/wks/git/'
alias gtj="cd /home/michaelgumowski/dev/wks/git/sonar-java/"
alias gths="cd /home/michaelgumowski/dev/wks/git/sonar-experiments/HeadShot-java11"

alias gh='git hist'
alias gri='git rebase --interactive'
alias grih2='git rebase --interactive HEAD~2'
alias grih10='git rebase --interactive HEAD~10'
grih() {
  gri HEAD~"$1"
}
alias gpr='git pull --rebase'
alias gco='git checkout'
alias gcom='git checkout master'
fix() {
  git add .
  git commit -am "fix"
}
# --------------------------------- work-related -----------------------------------------------------------------------
export SONARQUBE_LTS_7_9_VERSION="7.9.4"
export SONARQUBE_LTS_8_9_VERSION="8.9.0.43852"
export RULE_API_VERSION="2.1.0"
export SONAR_SCANNER_VERSION="4.0.0.1744"

export SONARQUBE_HOME="/home/michaelgumowski/dev/app/sonarqube"
export SONARQUBE_LTS_7_9_HOME="$SONARQUBE_HOME-$SONARQUBE_LTS_7_9_VERSION"
export SONARQUBE_LTS_8_9_HOME="$SONARQUBE_HOME-$SONARQUBE_LTS_8_9_VERSION"
export SONARQUBE_DEFAULT_HOME="$SONARQUBE_LTS_8_9_HOME"

export SONARQUBE_SCANNER_HOME="/home/michaelgumowski/dev/app/sonar-scanner-$SONAR_SCANNER_VERSION"

export ORCHESTRATOR_CONFIG_URL="file:///home/michaelgumowski/dev/orchestrator.properties"

alias sqLTS7="$SONARQUBE_LTS_7_9_VERSION/bin/linux-x86-64/./sonar.sh"
alias sqLTS8="$SONARQUBE_LTS_8_9_HOME/bin/linux-x86-64/./sonar.sh"
alias sq="$SONARQUBE_DEFAULT_HOME/bin/linux-x86-64/./sonar.sh"
alias sonarScanner='sonar-scanner'

alias yguard="java -jar /home/michaelgumowski/dev/app/yguard-2.6/lib/yguard.jar"

alias ruleAPI="java -jar /home/michaelgumowski/dev/wks/git/sonar-rule-api/target/rule-api-$RULE_API_VERSION-SNAPSHOT.jar"

sonarScannerDebug() {
  export SONAR_SCANNER_OPTS="-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000" && sonar-scanner
}

ruling(){
  java -version
  mci
  cd its/ruling
  mciWithTests
  cd ../..
}

sqRestartWithNewJavaPlugin() {
  javaPluginDirectory="/home/michaelgumowski/dev/wks/git/sonar-java/sonar-java-plugin/target/"
  sqPluginsDirectory="$SONARQUBE_DEFAULT_HOME/lib/extensions/"

  # drop java plugin from SQ
  find $sqPluginsDirectory -type f -name 'sonar-java-plugin*.jar' -delete

  # copy latest snapshot
  find $javaPluginDirectory -type f -name 'sonar-java-plugin*SNAPSHOT.jar' -exec cp '{}' $sqPluginsDirectory \;

  #sq restart
  $SONARQUBE_DEFAULT_HOME/bin/linux-x86-64/./sonar.sh restart
}

export PATH="$SONARQUBE_SCANNER_HOME/bin:$MAVEN_HOME/bin:$ANT_HOME/bin:$GOROOT/bin:$GOPATH/bin:$PATH"
# --------------------------------- jenv ; SHOULD BE LAST --------------------------------------------------------------

export PATH="/home/michaelgumowski/.jenv/bin:$PATH"
eval "$(jenv init -)"

# added by travis gem
[ -f /home/michaelgumowski/.travis/travis.sh ] && source /home/michaelgumowski/.travis/travis.sh

#THIS MUST BE AT THE END OF THE FILE FOR SDKMAN TO WORK!!!
export SDKMAN_DIR="/home/michaelgumowski/.sdkman"
[[ -s "/home/michaelgumowski/.sdkman/bin/sdkman-init.sh" ]] && source "/home/michaelgumowski/.sdkman/bin/sdkman-init.sh"
