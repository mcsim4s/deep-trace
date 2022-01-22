import { gql } from '@apollo/client';
export type Maybe<T> = T | null;
export type Exact<T extends { [key: string]: unknown }> = { [K in keyof T]: T[K] };
export type MakeOptional<T, K extends keyof T> = Omit<T, K> & { [SubKey in K]?: Maybe<T[SubKey]> };
export type MakeMaybe<T, K extends keyof T> = Omit<T, K> & { [SubKey in K]: Maybe<T[SubKey]> };
/** All built-in and custom scalars, mapped to their actual values */
export type Scalars = {
  ID: string;
  String: string;
  Boolean: boolean;
  Int: number;
  Float: number;
  Duration: any;
};

export type ClusterId = {
  __typename?: 'ClusterId';
  reportId: Scalars['String'];
  structureHash: Scalars['String'];
};

export type ClusterQueries = {
  __typename?: 'ClusterQueries';
  getCluster: TraceCluster;
};


export type ClusterQueriesGetClusterArgs = {
  reportId: Scalars['String'];
  structureHash: Scalars['String'];
};


export type Process = {
  __typename?: 'Process';
  start: Scalars['Duration'];
  duration: Scalars['Duration'];
  children: Array<Process>;
};

export type TraceCluster = {
  __typename?: 'TraceCluster';
  clusterId: ClusterId;
  rootProcess: Process;
};
