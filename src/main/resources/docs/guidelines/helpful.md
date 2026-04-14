## Run mvn clean verify
```bash
DB_USERNAME=inventory_user DB_PASSWORD=inventory_pass ./mvnw clean -B verify
```
## Clean up local branches where remote is already deleted
Run this one-liner to find and delete all local branches marked as [gone]:
```bash
git branch -vv | grep 'gone' | awk '{print $1}' | xargs git branch -D
```
    git branch -vv: Lists branches with their remote tracking status.
    grep ': gone]': Filters for branches whose remotes are missing.
    awk '{print $1}': Extracts just the branch name.
    xargs git branch -D: Force deletes the identified branches. 