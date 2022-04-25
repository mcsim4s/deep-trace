import {gql} from "@apollo/client";

export const ReportFragment = gql`
    fragment Report on AnalysisReport {
        id,
        createdAt,
        service,
        operation,
        state {
            ... on Clustering {
                __typename
            }

            ... on ClustersBuilt {
                __typename,
                clusterIds {
                    reportId,
                    structureHash
                }
            }
        }
    }
`

export const FlatStatsFr = gql`
    fragment FlatStatsFr on FlatStats {
        duration {
            average
        }
    }
`

export const ConcurrentStatsFr = gql`
    ${FlatStatsFr}
    fragment ConcurrentStatsFr on ConcurrentStats {
        avgSubprocesses,
        flat {
            ...FlatStatsFr
        }
    }
`

export const ParallelProcessFr = gql`
    ${FlatStatsFr}
    fragment ParallelProcessFr on ParallelProcess {
        id,
        isRoot,
        service,
        operation,
        childrenIds,
        stats {
            ...FlatStatsFr
        }
    }
`

export const SequentialProcessFr = gql`
    fragment SequentialProcessFr on SequentialProcess {
        id,
        childrenIds
    }
`

export const ConcurrentProcessFr = gql`
    ${ConcurrentStatsFr}
    fragment ConcurrentProcessFr on ConcurrentProcess {
        id,
        ofId,
        stats {
            ...ConcurrentStatsFr
        }
    }
`

export const GapFr = gql`
    ${FlatStatsFr}
    fragment GapFr on Gap {
        id,
        stats {
            ...FlatStatsFr
        }
    }
`

export const ProcessFragment = gql`
    ${ParallelProcessFr}
    ${ConcurrentProcessFr}
    ${SequentialProcessFr}
    ${GapFr}
    fragment ProcessFields on Process {
        __typename,
        ... on ParallelProcess {
            ...ParallelProcessFr
        }
        ... on ConcurrentProcess {
            ...ConcurrentProcessFr
        }
        ... on SequentialProcess {
            ...SequentialProcessFr
        }
        ... on Gap {
            ...GapFr
        }
    }
`

export const ClusterFragment = gql`
    ${ProcessFragment}
    fragment Cluster on TraceCluster {
        id {
            reportId,
            structureHash
        },
        exampleTraceId,
        processes {
            key,
            value {
                ...ProcessFields
            }
        }
    }
`