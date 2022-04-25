import {FlatStats, KvStringProcess, ParallelProcess, Process} from "../generated/graphql";

export class Trace {
    root: ParallelProcess;
    processes: Map<String, Process> = new Map<String, Process>()

    constructor(processes: Array<KvStringProcess>) {
        let rootRef: ParallelProcess | null = null
        processes.forEach(node => {
            if (node.value.__typename === "ParallelProcess") {
                if (node.value.isRoot) {
                    rootRef = node.value
                }
            }
            this.processes.set(node.key, node.value)
        })

        if (rootRef) {
            this.root = rootRef
        } else {
            throw new Error(`Illegal process initialization. No root process in ${JSON.stringify(processes)}`)
        }
    }

    private isProcess(pr: Process | undefined): pr is Process {
        return !!pr;
    }

    childrenOf(process: Process): Process[] {
        switch (process.__typename) {
            case "ParallelProcess":
                return process.childrenIds.map(id => this.processes.get(id)).filter(this.isProcess);
            case "Gap":
                return [];
            case "ConcurrentProcess":
                return [this.processes.get(process.ofId)].filter(this.isProcess);
            case "SequentialProcess":
                return process.childrenIds.map(id => this.processes.get(id)).filter(this.isProcess);
            default:
                throw new Error("Unknown process type")
        }
    }

    get(id: String): Process {
        const res = this.processes.get(id);
        if (res) {
            return res;
        } else {
            throw new Error(`No process with ${id} in trace`)
        }
    }

    flatStatsOf(process: Process): FlatStats {
        switch (process.__typename) {
            case "SequentialProcess":
                throw new Error("Trying to access flat stats of an Sequential process")
            case "Gap":
                return process.stats;
            case "ConcurrentProcess":
                return process.stats.flat;
            case "ParallelProcess":
                return process.stats;
            default:
                throw new Error("Unknown process type");
        }
    }

    serviceOf(process: Process): string {
        switch (process.__typename) {
            case "ParallelProcess":
                return process.service;
            case "ConcurrentProcess":
                return this.asParallel(this.get(process.ofId)).service;
            default:
                throw new Error(`Trying to get service name of ${process.__typename}`);
        }
    }

    operationOf(process: Process): string {
        switch (process.__typename) {
            case "ParallelProcess":
                return process.operation;
            case "ConcurrentProcess":
                return this.asParallel(this.get(process.ofId)).operation;
            default:
                throw new Error(`Trying to get service name of ${process.__typename}`);
        }
    }

    asParallel(process: Process): ParallelProcess {
        switch (process.__typename) {
            case "ParallelProcess":
                return process;
            default:
                throw new Error(`Trying to convert ${process.__typename} to ParallelProcess`);
        }
    }
}