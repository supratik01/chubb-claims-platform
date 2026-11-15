/**
 * Readable names for the types generated from the OpenAPI contract.
 * Never edit these shapes by hand: change claims-api.yaml and run `npm run generate:api`.
 */
import type { components, paths } from './claims-api.generated';

type Schemas = components['schemas'];

export type Role = Schemas['Role'];
export type ClaimStatus = Schemas['ClaimStatus'];
export type LineOfBusiness = Schemas['LineOfBusiness'];
export type LoginRequest = Schemas['LoginRequest'];
export type LoginResponse = Schemas['LoginResponse'];
export type UserSummary = Schemas['UserSummary'];
export type PolicySummary = Schemas['PolicySummary'];
export type SubmitClaimRequest = Schemas['SubmitClaimRequest'];
export type ClaimSummary = Schemas['ClaimSummary'];
export type ClaimDetail = Schemas['ClaimDetail'];
export type StatusHistoryEntry = Schemas['StatusHistoryEntry'];
export type InformationRequest = Schemas['InformationRequest'];
export type CreateInformationRequest = Schemas['CreateInformationRequest'];
export type InformationResponse = Schemas['InformationResponse'];
export type DecisionRequest = Schemas['DecisionRequest'];
export type ClaimPage = Schemas['ClaimPage'];
export type WorkloadSummary = Schemas['WorkloadSummary'];
export type OfficerWorkload = Schemas['OfficerWorkload'];
export type ExposureSummary = Schemas['ExposureSummary'];
export type CurrencyExposure = Schemas['CurrencyExposure'];
export type ExposureLine = Schemas['ExposureLine'];
export type Problem = Schemas['Problem'];
export type ErrorCode = Problem['code'];

type ListClaimsQuery = NonNullable<paths['/claims']['get']['parameters']['query']>;
export type Assignment = NonNullable<ListClaimsQuery['assignment']>;
export type ClaimListQuery = ListClaimsQuery;
