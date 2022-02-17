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
        start,
        duration
    }

    fragment ProcessRecursive on Process {
        ...ProcessFields,
        children {
            ...ProcessFields
            children {
                ...ProcessFields
                children {
                    ...ProcessFields
                    children {
                        ...ProcessFields
                    }
                }
            }
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
        rootProcess {
            ...ProcessRecursive
        }
    }
`