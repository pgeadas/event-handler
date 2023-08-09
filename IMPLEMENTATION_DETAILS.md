# Implementation details

The solution presented makes use of two different techniques for query optimization, which I will be covering below.

## Keyset pagination

Since we have a unique identifier for each row of the database (the id), we can improve our query time (especially for
large datasets) by filtering our results directly by it. This operation is usually faster than relying on the OFFSET,
since the latter needs to skip N amount of rows until it reaches the desired one, which still involves some processing.
This can obviously be a bigger issue for very large datasets, therefore it was avoided.

Additionally, since the id is the primary key which is indexed by default, using keyset pagination will be very
efficient (it works better on indexed columns). Also, another advantage of using the id (which is auto incremented) is
that we will not suffer from potential race condition issues like we could when we use OFFSET (since a value could be
added in, between the time that we query and the time that the database seeks the desired position).

### Possible downsides

In the end, the decision was taken after weighting all the advantages and disadvantages of both approaches considered,
keyset and offset pagination (taking into account the size of the dataset and the probability of some of them
happening).
Even though I decided to move forward with keyset pagination, there are some points where it might struggle:

1. **Table schema changes**: If we need to change the schema of the database and decide to remove or change the "id"
   type or use another field as primary key, we will need to adapt our solution which involves changing the code (offset
   pagination does not suffer from this, as it does not care about columns) and possibly adding indexes to the columns
   used as keys, in order to keep our solution performant. Depending on the new "id" type, it might not even be possible
   to use keyset pagination, which works on the premise of having one or more columns that provide a unique and ordered
   identifier;

2. **Missing records**: We can still see some pages with lesser items than intended if a delete happens in the middle of
   a query. If we are unlucky, the deleted row is one that is already added to the ResultSet while the query is still
   fetching the remaining rows, so either we are fetching a value that is not in the database anymore, or we will have
   one less item in the final result.
   This depends on the strategy used for concurrency and if we are using locks or not, but considering the current
   implementation this could be an issue since we are not using any kind of concurrency control. (Both approaches can
   suffer from this, even though, for large datasets, it is more likely to happen using OFFSET since we need to skip a
   huge amount of rows to reach our target);

3. **Sorting**: We are only allowing sorting AFTER we fetch the respective number of items. Another option would be to
   sort BEFORE we fetch the LIMIT amount of items, but to do that efficiently we would need to have indexes in those
   columns (both approaches suffer from the lack of indexes in this case). However, I think it is worth saying that if
   we had more indexed columns, adapting the solution would be trivial: we could ask the user for an extra parameter,
   indicating if the sort should be done before or after the filtering;

4. **ID size**: For very large datasets we would need to use a BigInt for the id, which consumes more space, but would
   avoid overflow which would break our solution (Offset pagination does not suffer from this);

5. **Multiple-table queries**: If we had multiple tables and complex join operations, maintaining the same order of
   results could be tricky (nevertheless, both approaches suffer from this).

## Prepared statements

This technique involves preparing the statements before we even execute them and sending them to the database to be
compiled beforehand, using placeholders for values, which are only set latter when we receive the requests. This way,
since the database already has the query stored, we avoid resending it every time which will also speed up the query
time.

The only caveat (at least using the default Groovy Sql lib) is that this technique can only be used when what vary
are values. Operations like orderBy or sort direction cannot be set dynamically this way, so for those queries this
improvement could not be used.

This technique is also used to sanitize queries and avoiding SQL injection, but in this case it was not needed since we
already do all validations beforehand.

## Tests

There are three types of tests:

### Unit tests

These are applied to the domain objects and their goal is to test the business logic. They intend to test specific
classes in isolation and making sure that the domain objects can be created correctly and are validated.

### Integration tests

These aim to test the interactions between our controllers and use cases, since the moment we receive a request until
we have to call our infrastructure for results. Usually we can mock the infrastructure part, since infra is totally
independent from both application and domain and is tested separately. Our main focus here is to verify that our
controllers are receiving and processing the requests correctly, forwarding it to the use cases and receiving the
expected responses.

### Infrastructure

These tests focus on testing the different adapters that implement our domain interfaces (ports). Since we are
testing the contract between adapters and ports, it showuld not matter what is the actual underlying technology, so
the tests should be exactly the same and won't need to change at all. The only thing that is needed when adding a new
adapter is to populate them with the same test data.

In our case we have two adapters:

- SqlEventRepository
- InMemoryEventRepository (only used in tests)

## TODOs

There is a list of improvement ideas that are not directly related to the given requirements, but rather some ideas for
future work:

- Logger instead of print
- Async processing of requests (queue)
- Flyway for automatic database migrations
- Wrapper class for response data
- Tests for the load of properties files from both local environments and from jars (currently not working from jar)
