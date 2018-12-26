Extract all commits metadata from SonarSourcers contributing to the Language Team repositories.

To run the export:
```
mvn exec:java

```
Produces JSON reports for each repository in folder `target/results`.


Example of query of the GitHub REST API to collect all the commits from sonar-java since 2017/01/01:
```
curl -i 'https://api.github.com/repos/SonarSource/sonar-java/commits?since=2017-01-01' > res.txt
```