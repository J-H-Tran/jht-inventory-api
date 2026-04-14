# jht-inventory-api
API Design

Git flow:
feature/xyz branch from develop
feature/xyz -> develop (merge --no-ff)
release/vX.X branch from develop (testing, bugfixes)
release/vX.X -> master (merge --no-ff + tag), release/vX.X (delete)
master -> develop (back-merge to develop, sync + tag visibility), bypass for admins