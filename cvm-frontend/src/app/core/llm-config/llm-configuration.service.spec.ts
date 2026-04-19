import {
  HttpTestingController,
  provideHttpClientTesting
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { AppConfigService } from '../config/app-config.service';
import { LlmConfigurationService } from './llm-configuration.service';

describe('LlmConfigurationService', () => {
  let service: LlmConfigurationService;
  let http: HttpTestingController;

  beforeEach(() => {
    const config = jasmine.createSpyObj<AppConfigService>(
      'AppConfigService',
      ['get']
    );
    config.get.and.returnValue({
      apiBaseUrl: 'http://api.test',
      keycloak: { url: '', realm: '', clientId: '' }
    } as any);
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AppConfigService, useValue: config }
      ]
    });
    service = TestBed.inject(LlmConfigurationService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('list() ruft GET /api/v1/admin/llm-configurations auf', async () => {
    const promise = service.list();
    const req = http.expectOne(
      'http://api.test/api/v1/admin/llm-configurations'
    );
    expect(req.request.method).toBe('GET');
    req.flush([]);
    const res = await promise;
    expect(Array.isArray(res)).toBeTrue();
  });

  it('providers() ruft GET /providers auf', async () => {
    const promise = service.providers();
    const req = http.expectOne(
      'http://api.test/api/v1/admin/llm-configurations/providers'
    );
    expect(req.request.method).toBe('GET');
    req.flush([
      { provider: 'openai', defaultBaseUrl: 'u', requiresExplicitBaseUrl: false }
    ]);
    const res = await promise;
    expect(res.length).toBe(1);
  });

  it('create() postet Request-Body', async () => {
    const body = {
      name: 'OpenAI Prod',
      description: null,
      provider: 'openai',
      model: 'gpt-4o',
      baseUrl: null,
      secret: 'sk-xxx',
      maxTokens: 1024,
      temperature: 0.3,
      active: true
    };
    const promise = service.create(body);
    const req = http.expectOne(
      'http://api.test/api/v1/admin/llm-configurations'
    );
    expect(req.request.method).toBe('POST');
    expect(req.request.body.name).toBe('OpenAI Prod');
    req.flush({
      id: 'id-1',
      tenantId: 't-1',
      name: 'OpenAI Prod',
      description: null,
      provider: 'openai',
      model: 'gpt-4o',
      baseUrl: 'https://api.openai.com/v1',
      secretSet: true,
      secretHint: '****xxx',
      maxTokens: 1024,
      temperature: 0.3,
      active: true,
      createdAt: '2026-04-19T10:00:00Z',
      updatedAt: '2026-04-19T10:00:00Z',
      updatedBy: 'a.admin@ahs.test'
    });
    const res = await promise;
    expect(res.id).toBe('id-1');
  });

  it('update() sendet PUT mit Teil-Update', async () => {
    const promise = service.update('id-1', { active: true });
    const req = http.expectOne(
      'http://api.test/api/v1/admin/llm-configurations/id-1'
    );
    expect(req.request.method).toBe('PUT');
    expect(req.request.body.active).toBeTrue();
    req.flush({
      id: 'id-1',
      tenantId: 't-1',
      name: 'x',
      description: null,
      provider: 'openai',
      model: 'gpt-4o',
      baseUrl: null,
      secretSet: false,
      secretHint: null,
      maxTokens: null,
      temperature: null,
      active: true,
      createdAt: 'a',
      updatedAt: 'b',
      updatedBy: 'c'
    });
    const res = await promise;
    expect(res.active).toBeTrue();
  });

  it('delete() sendet DELETE ohne Body', async () => {
    const promise = service.delete('id-1');
    const req = http.expectOne(
      'http://api.test/api/v1/admin/llm-configurations/id-1'
    );
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
    await expectAsync(promise).toBeResolved();
  });

  it('testAdhoc() postet an /test und liefert TestResult', async () => {
    const promise = service.testAdhoc({
      provider: 'openai',
      model: 'gpt-4o',
      baseUrl: null,
      secret: 'sk-test'
    });
    const req = http.expectOne(
      'http://api.test/api/v1/admin/llm-configurations/test'
    );
    expect(req.request.method).toBe('POST');
    expect(req.request.body.provider).toBe('openai');
    req.flush({
      success: true,
      provider: 'openai',
      model: 'gpt-4o',
      httpStatus: 200,
      latencyMs: 42,
      message: 'OK'
    });
    const res = await promise;
    expect(res.success).toBeTrue();
    expect(res.httpStatus).toBe(200);
  });

  it('testSaved() postet an /{id}/test mit Overrides', async () => {
    const promise = service.testSaved('id-9', { model: 'gpt-4o-mini' });
    const req = http.expectOne(
      'http://api.test/api/v1/admin/llm-configurations/id-9/test'
    );
    expect(req.request.method).toBe('POST');
    expect(req.request.body.model).toBe('gpt-4o-mini');
    req.flush({
      success: false,
      provider: 'openai',
      model: 'gpt-4o-mini',
      httpStatus: 401,
      latencyMs: 12,
      message: 'HTTP 401: Unauthorized'
    });
    const res = await promise;
    expect(res.success).toBeFalse();
    expect(res.message).toContain('401');
  });

  it('testSaved() ohne Overrides postet leeren Body', async () => {
    const promise = service.testSaved('id-9');
    const req = http.expectOne(
      'http://api.test/api/v1/admin/llm-configurations/id-9/test'
    );
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({});
    req.flush({
      success: true,
      provider: 'ollama',
      model: 'llama3',
      httpStatus: 200,
      latencyMs: 5,
      message: 'OK'
    });
    await expectAsync(promise).toBeResolved();
  });
});
