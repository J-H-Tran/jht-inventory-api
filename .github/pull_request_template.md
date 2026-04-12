## Summary
&lt;!-- What does this PR accomplish? --&gt;

## Design Decisions
&lt;!-- Key trade-offs, why this approach --&gt;

## Migration/Schema Changes
- [ ] No schema changes
- [ ] New migration file added (V{next}__description.sql)
- [ ] Backwards compatible

## Testing
- [ ] Unit tests
- [ ] Integration tests (TestContainers)
- [ ] Manual verification

## Definition of Done
- [ ] Code compiles and application starts cleanly
- [ ] Flyway migrations run without errors on a clean DB
- [ ] All new endpoints return RFC 7807-compliant error responses
- [ ] No raw Exception types thrown from service or controller layer — only typed exceptions from `shared.exception`
- [ ] @Transactional boundaries are intentional and commented where non-obvious

## Checklist
- [ ] `mvn clean verify` passes
- [ ] Branch up to date with `develop`
- [ ] No merge conflicts
- [ ] Commits follow project conventions