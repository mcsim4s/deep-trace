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
  Duration: number;
};

export type AnalysisReport = {
  __typename?: 'AnalysisReport';
  id: Scalars['String'];
  name: Scalars['String'];
};

export type ClusterId = {
  __typename?: 'ClusterId';
  reportId: Scalars['String'];
  structureHash: Scalars['String'];
};


export type Mutations = {
  __typename?: 'Mutations';
  createReport?: Maybe<AnalysisReport>;
};


export type MutationsCreateReportArgs = {
  sourceId: Scalars['String'];
  params: TraceQueryInput;
};

export type Process = {
  __typename?: 'Process';
  start: Scalars['Duration'];
  duration: Scalars['Duration'];
  children: Array<Process>;
};

export type Queries = {
  __typename?: 'Queries';
  getCluster: TraceCluster;
  listReports?: Maybe<Array<AnalysisReport>>;
};


export type QueriesGetClusterArgs = {
  reportId: Scalars['String'];
  structureHash: Scalars['String'];
};

export type TraceCluster = {
  __typename?: 'TraceCluster';
  clusterId: ClusterId;
  rootProcess: Process;
};

export type TraceQueryInput = {
  serviceName: Scalars['String'];
  operationName: Scalars['String'];
  tags: Array<Scalars['String']>;
  startTimeMinSeconds?: Maybe<Scalars['Int']>;
  startTimeMaxSeconds?: Maybe<Scalars['Int']>;
  durationMinMillis?: Maybe<Scalars['Int']>;
  durationMaxMillis?: Maybe<Scalars['Int']>;
};
