import {FlatStats, ParallelProcess, Process} from "../generated/graphql";

export default {
    flatStatsOf: function (process: Process): FlatStats {
        switch (process.__typename) {
            case "SequentialProcess": throw new Error("Trying to access flat stats of an Sequential process")
            case "Gap": return process.stats;
            case "ConcurrentProcess": return process.stats.flat;
            case "ParallelProcess": return process.stats;
            default: throw new Error("Unknown process type");
        }
    },

    serviceOf: function (process: Process): String {
        switch (process.__typename) {
            case "ParallelProcess": return process.service;
            default: throw new Error(`Trying to get service name of ${process.__typename}`);
        }
    },

    asParallel(process: Process): ParallelProcess {
        switch (process.__typename) {
            case "ParallelProcess": return process;
            default: throw new Error(`Trying to convert ${process.__typename} to ParallelProcess`);
        }
    }
}