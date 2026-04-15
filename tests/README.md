# Why separate tests module?

The tests use the main annotation processor internally. Maven compiler plugin does not allow "test" scope annotation
processors. Therefore, the main module must be first built and installed to the local repository before the tests can be
compiled and run.