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
  /** An instantaneous point on the time-line represented by a standard date time string */
  Instant: any;
};

export type AnalysisReport = {
  __typename?: 'AnalysisReport';
  id: Scalars['String'];
  createdAt: Scalars['Instant'];
  service: Scalars['String'];
  operation: Scalars['String'];
  state: State;
};

export type ClusterId = {
  __typename?: 'ClusterId';
  reportId: Scalars['String'];
  structureHash: Scalars['String'];
};

export type Clustering = {
  __typename?: 'Clustering';
  /** Fake field because GraphQL does not support empty objects. Do not query, use __typename instead. */
  _?: Maybe<Scalars['Boolean']>;
};

export type ClustersBuilt = {
  __typename?: 'ClustersBuilt';
  clusterIds: Array<ClusterId>;
};



/** A key-value pair of String and Process */
export type KvStringProcess = {
  __typename?: 'KVStringProcess';
  /** Key */
  key: Scalars['String'];
  /** Value */
  value: Process;
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
  id: Scalars['String'];
  service: Scalars['String'];
  operation: Scalars['String'];
  parentId?: Maybe<Scalars['String']>;
  stats: ProcessStats;
};

export type ProcessStats = {
  __typename?: 'ProcessStats';
  avgStart: Scalars['Duration'];
  avgDuration: Scalars['Duration'];
  allDurations: Array<Scalars['Duration']>;
};

export type Queries = {
  __typename?: 'Queries';
  getCluster: TraceCluster;
  listReports?: Maybe<Array<AnalysisReport>>;
  getReport?: Maybe<AnalysisReport>;
};


export type QueriesGetClusterArgs = {
  reportId: Scalars['String'];
  structureHash: Scalars['String'];
};


export type QueriesGetReportArgs = {
  value: Scalars['String'];
};

export type State = Clustering | ClustersBuilt;

export type TraceCluster = {
  __typename?: 'TraceCluster';
  id: ClusterId;
  processes: Array<KvStringProcess>;
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
