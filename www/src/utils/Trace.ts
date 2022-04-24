import {KvStringProcess, ParallelProcess, Process} from "../generated/graphql";

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

        console.log(this.processes.values())
    }

    private isProcess(pr: Process | undefined): pr is Process { return !!pr; }

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
}