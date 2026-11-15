import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, InjectionToken, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  ClaimDetail,
  ClaimListQuery,
  ClaimPage,
  CreateInformationRequest,
  DecisionRequest,
  ExposureSummary,
  InformationResponse,
  LoginRequest,
  LoginResponse,
  PolicySummary,
  SubmitClaimRequest,
  WorkloadSummary,
} from './models';

/** Where the claims API is mounted. The web server proxies this path to the backend, so it is same-origin. */
export const API_BASE_URL = new InjectionToken<string>('API_BASE_URL', {
  providedIn: 'root',
  factory: () => '/api/v1',
});

/**
 * The only class in the frontend that knows URLs. One method per operation in claims-api.yaml, named after its
 * operationId, so a reviewer can check the two side by side.
 */
@Injectable({ providedIn: 'root' })
export class ClaimsApiClient {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  login(body: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.baseUrl}/auth/login`, body);
  }

  listMyPolicies(): Observable<PolicySummary[]> {
    return this.http.get<PolicySummary[]>(`${this.baseUrl}/policies`);
  }

  submitClaim(body: SubmitClaimRequest): Observable<ClaimDetail> {
    return this.http.post<ClaimDetail>(`${this.baseUrl}/claims`, body);
  }

  listClaims(query: ClaimListQuery = {}): Observable<ClaimPage> {
    let params = new HttpParams();
    if (query.assignment) params = params.set('assignment', query.assignment);
    if (query.status?.length) params = params.appendAll({ status: query.status });
    if (query.page !== undefined) params = params.set('page', query.page);
    if (query.size !== undefined) params = params.set('size', query.size);
    return this.http.get<ClaimPage>(`${this.baseUrl}/claims`, { params });
  }

  getClaim(claimId: string): Observable<ClaimDetail> {
    return this.http.get<ClaimDetail>(`${this.baseUrl}/claims/${claimId}`);
  }

  pickUpClaim(claimId: string): Observable<ClaimDetail> {
    return this.http.post<ClaimDetail>(`${this.baseUrl}/claims/${claimId}/assignment`, null);
  }

  requestInformation(claimId: string, body: CreateInformationRequest): Observable<ClaimDetail> {
    return this.http.post<ClaimDetail>(
      `${this.baseUrl}/claims/${claimId}/information-requests`,
      body,
    );
  }

  respondToInformationRequest(
    claimId: string,
    requestId: string,
    body: InformationResponse,
  ): Observable<ClaimDetail> {
    return this.http.post<ClaimDetail>(
      `${this.baseUrl}/claims/${claimId}/information-requests/${requestId}/response`,
      body,
    );
  }

  decideClaim(claimId: string, body: DecisionRequest): Observable<ClaimDetail> {
    return this.http.post<ClaimDetail>(`${this.baseUrl}/claims/${claimId}/decision`, body);
  }

  settleClaim(claimId: string): Observable<ClaimDetail> {
    return this.http.post<ClaimDetail>(`${this.baseUrl}/claims/${claimId}/settlement`, null);
  }

  getWorkload(): Observable<WorkloadSummary> {
    return this.http.get<WorkloadSummary>(`${this.baseUrl}/dashboard/workload`);
  }

  getExposure(): Observable<ExposureSummary> {
    return this.http.get<ExposureSummary>(`${this.baseUrl}/dashboard/exposure`);
  }
}
