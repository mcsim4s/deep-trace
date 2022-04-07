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

export const ProcessFragment = gql`
    fragment ProcessFields on Process {
        id,
        service,
        operation,
        parentId,
        stats {
            avgStart,
            avgDuration,
            allDurations
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
        processes {
            key,
            value {
                ...ProcessFields
            }
        }
    }
`