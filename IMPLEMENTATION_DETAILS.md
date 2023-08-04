# Implementation details

The solution presented makes use of two different techniques for query optimization:

1. Keyset pagination

   Since we have a unique identifier for each row of the database (the id), we can improve our query time (especially for large datasets) by filtering our results directly by it. This operation is usually faster than relying on the OFFSET, since the latter needs to skip N amount of rows until it reaches the desired one, which still involves some processing. This can obviously be a bigger issue for very large datasets, therefore it was avoided.
    Additionally, since the id is the primary key which is indexed by default, using keyset pagination will be very efficient (it works better on indexed columns). Also, another advantage of using the id (which is auto incremented) is that we will not suffer from potential race condition issues like we could when we use OFFSET (since a value could be added in, between the time that we query and the time that the database seeks the desired position). 

2. Prepared statements

   This technique involves preparing the statements before we even execute them and sending them to the database to be compiled beforehand, using placeholders for values, which are only set latter when we receive the requests. This way, since the database already has the query stored, we avoid resending it every time which will also speed up the query time. The only caveat (at least using the default Groovy Sql lib) is that this technique can only be used when what vary are values. Operations like orderBy or sort direction cannot be set dynamically this way, so for those queries this improvement could not be used. 
    This technique is also used to sanitize queries and avoiding SQL injection, but in this case it was not needed since we already do all validations beforehand.

# Tests

There are three types of tests: 

1. Unit tests

    These are applied to the domain objects and their goal is to test the business logic. They intend to test specific classes in isolation and making sure that the domain objects can be created correctly and are validated.

2. Integration tests

    These aim to test the interactions between our controllers and use cases, since the moment we receive a request until we have to call our infrastructure for results. Usually we can mock the infrastructure part, since infra is totally independent from both application and domain and is tested separately. Our main focus here is to verify that our controllers are receiving and processing the requests correctly, forwarding it to the use cases and receiving the expected responses.

3. Infrastructure
   
    These tests focus on testing the different adapters that implement our domain interfaces (ports). Since we are testing the contract between adapters and ports, it showuld not matter what is the actual underlying technology, so the tests should be exactly the same and won't need to change at all. The only thing that is needed when adding a new adapter is to populate them with the same test data.  

    In our case we have two adapters:
    - SqlEventRepository
    - InMemoryEventRepository (only used in tests)

# TODOs

There is a list of improvement ideas that are not directly related to the given requirements, but rather some ideas for future work:

- Logger instead of print
- Async processing of requests (queue)
- Flyway for automatic database migrations
- Wrapper class for response data
- Tests for the load of properties files from both local environments and from jars (currently not working from jar)
