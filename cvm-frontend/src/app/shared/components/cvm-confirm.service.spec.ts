import { TestBed } from '@angular/core/testing';
import { CvmConfirmHostComponent, CvmConfirmService } from './cvm-confirm.service';

describe('CvmConfirmHostComponent', () => {
  let host: CvmConfirmHostComponent;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    const fixture = TestBed.createComponent(CvmConfirmHostComponent);
    host = fixture.componentInstance;
  });

  it('open haelt den Request vor und setzt current()', async () => {
    const p = host.open({ title: 't', message: 'm' });
    expect(host.current()?.title).toBe('t');
    expect(host.current()?.message).toBe('m');
    host.bestaetigen();
    await expectAsync(p).toBeResolvedTo(true);
    expect(host.current()).toBeNull();
  });

  it('abbrechen liefert false zurueck', async () => {
    const p = host.open({ title: 't', message: 'm' });
    host.abbrechen();
    await expectAsync(p).toBeResolvedTo(false);
    expect(host.current()).toBeNull();
  });

  it('zweiter abbrechen-Aufruf ohne offenen Request tut nichts', () => {
    expect(() => host.abbrechen()).not.toThrow();
    expect(host.current()).toBeNull();
  });
});

describe('CvmConfirmService', () => {
  afterEach(() => {
    document.body.querySelectorAll('cvm-confirm-host').forEach((el) => el.remove());
  });

  it('confirm legt Host lazy an und leitet bestaetigen durch', async () => {
    TestBed.configureTestingModule({});
    const svc = TestBed.inject(CvmConfirmService);

    const promise = svc.confirm({ title: 'Weg?', message: 'Sicher?' });
    const host = (svc as unknown as { host: CvmConfirmHostComponent }).host;
    expect(host).toBeDefined();
    expect(host.current()?.title).toBe('Weg?');

    host.bestaetigen();
    await expectAsync(promise).toBeResolvedTo(true);
  });

  it('zweiter confirm-Aufruf wiederverwendet denselben Host', async () => {
    TestBed.configureTestingModule({});
    const svc = TestBed.inject(CvmConfirmService);

    const p1 = svc.confirm({ title: 'A', message: 'a' });
    const hostA = (svc as unknown as { host: CvmConfirmHostComponent }).host;
    hostA.abbrechen();
    await expectAsync(p1).toBeResolvedTo(false);

    const p2 = svc.confirm({ title: 'B', message: 'b' });
    const hostB = (svc as unknown as { host: CvmConfirmHostComponent }).host;
    expect(hostB).toBe(hostA);
    hostB.bestaetigen();
    await expectAsync(p2).toBeResolvedTo(true);
  });
});
