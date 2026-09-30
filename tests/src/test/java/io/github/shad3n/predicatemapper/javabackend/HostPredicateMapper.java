package io.github.shad3n.predicatemapper.javabackend;

import io.github.shad3n.predicatemapper.annotation.PredicateMapper;
import io.github.shad3n.predicatemapper.annotation.ToJavaPredicateMapper;

import java.util.function.Predicate;

@PredicateMapper
public interface HostPredicateMapper {

    @ToJavaPredicateMapper(Host.class)
    Predicate<Host> hosts(HostFilter filter);

    @ToJavaPredicateMapper(Server.class)
    Predicate<Server> servers(ServerFilter filter);
}
