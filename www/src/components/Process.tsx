import React from 'react';
import { useQuery, gql } from '@apollo/client';
import { Heading } from 'react-bulma-components';
import {ClusterQueries, ClusterQueriesGetClusterArgs} from "../generated/graphql";

export default function Process() {
  let { loading, error, data } = useQuery<ClusterQueries, ClusterQueriesGetClusterArgs>(
    gql`
      fragment ProcessFields on Process {
        start,
        duration
      }
      
      fragment ProcessRecursive on Process {
        ...ProcessFields,
        children {
          ...ProcessFields
        }
      }
      
      query GetCluster($reportId: String!, $structureHash: String!) {
        getCluster(reportId: $reportId, structureHash: $structureHash) {
          clusterId {
            reportId,
            structureHash
          },
          rootProcess {
            ...ProcessRecursive
          }
        }
      }
    `,
    { variables: { reportId: "test", structureHash: "test" } }
  );
  if (error) throw error;
  if (loading) return <Heading size={4}>Loading ...</Heading>;
  if (!data) throw new Error("no data");

  return <>
    <div>
      <Heading size={1}>Cluster</Heading>
      <Heading size={4}>Cluster id: {JSON.stringify(data.getCluster.clusterId)}</Heading>
    </div>
  </>;
};