import {KvStringProcess, Process} from "../generated/graphql";

export class Trace {
    root: Process;
    processes: Map<String, Process> = new Map<String, Process>();
    childMap: Map<String, Process[]> = new Map<String, Process[]>()

    constructor(processes: Array<KvStringProcess>) {
        let rootRef: Process | null = null
        processes.forEach(node => {
            if (!node.value.parentId) {
                rootRef = node.value
            }
            this.processes.set(node.key, node.value)
            if(node.value.parentId) {
                const id: string = node.value.parentId
                const children: Process[] = this.childMap.get(id) || [];
                children.push(node.value)
                this.childMap.set(id, children)
            }

        })

        if (rootRef) {
            this.root = rootRef
        } else {
            throw new Error(`Illegal process initialization. No root process in ${JSON.stringify(processes)}`)
        }
    }

    childrenOf(process: Process): Process[] {
        return this.childMap.get(process.id) || [];
    }
}