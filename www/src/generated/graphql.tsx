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

export type ConcurrentProcess = {
  __typename?: 'ConcurrentProcess';
  id: Scalars['String'];
  ofId: Scalars['String'];
  stats: ConcurrentStats;
};

export type ConcurrentStats = {
  __typename?: 'ConcurrentStats';
  flat: FlatStats;
  avgSubprocesses: Scalars['Float'];
};


export type DurationStats = {
  __typename?: 'DurationStats';
  average: Scalars['Duration'];
};

export type FlatStats = {
  __typename?: 'FlatStats';
  duration: DurationStats;
};

export type Gap = {
  __typename?: 'Gap';
  id: Scalars['String'];
  stats: FlatStats;
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

export type OperationSuggest = {
  __typename?: 'OperationSuggest';
  name: Scalars['String'];
  kind: Scalars['String'];
};

export type ParallelProcess = {
  __typename?: 'ParallelProcess';
  id: Scalars['String'];
  isRoot: Scalars['Boolean'];
  service: Scalars['String'];
  operation: Scalars['String'];
  childrenIds: Array<Scalars['String']>;
  stats: FlatStats;
};

export type Process = ConcurrentProcess | Gap | ParallelProcess | SequentialProcess;

export type Queries = {
  __typename?: 'Queries';
  getCluster: TraceCluster;
  listReports?: Maybe<Array<AnalysisReport>>;
  getReport?: Maybe<AnalysisReport>;
  suggest: SuggestResponse;
};


export type QueriesGetClusterArgs = {
  reportId: Scalars['String'];
  structureHash: Scalars['String'];
};


export type QueriesGetReportArgs = {
  value: Scalars['String'];
};


export type QueriesSuggestArgs = {
  serviceName?: Maybe<Scalars['String']>;
};

export type SequentialProcess = {
  __typename?: 'SequentialProcess';
  id: Scalars['String'];
  childrenIds: Array<Scalars['String']>;
};

export type State = Clustering | ClustersBuilt;

export type SuggestResponse = {
  __typename?: 'SuggestResponse';
  services: Array<Scalars['String']>;
  operations: Array<OperationSuggest>;
};

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
