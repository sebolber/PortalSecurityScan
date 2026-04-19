import { TestBed } from '@angular/core/testing';
import { AdminProductsComponent } from './admin-products.component';
import {
  ProductsService,
  ProductView
} from '../../core/products/products.service';
import { CvmToastService } from '../../shared/components/cvm-toast.service';

class FakeToast {
  success = jasmine.createSpy('success');
  warning = jasmine.createSpy('warning');
  error = jasmine.createSpy('error');
}

/**
 * Iteration 89 (CVM-329): Edit-Dialog ersetzt die bisherige
 * window.prompt-Kette fuer Produkt-Bearbeitung.
 */
describe('AdminProductsComponent - Iteration 89 (Edit-Dialog)', () => {
  let products: jasmine.SpyObj<ProductsService>;
  let toast: FakeToast;

  const beispielProdukt: ProductView = {
    id: 'p1',
    key: 'portalcore-test',
    name: 'PortalCore-Test',
    description: 'Alte Beschreibung',
    repoUrl: null
  };

  beforeEach(() => {
    products = jasmine.createSpyObj<ProductsService>('ProductsService', [
      'list',
      'versions',
      'create',
      'update',
      'delete',
      'createVersion',
      'deleteVersion'
    ]);
    products.list.and.returnValue(Promise.resolve([]));
    toast = new FakeToast();

    TestBed.configureTestingModule({
      imports: [AdminProductsComponent],
      providers: [
        { provide: ProductsService, useValue: products },
        { provide: CvmToastService, useValue: toast }
      ]
    });
  });

  it('bearbeiteProdukt oeffnet den Dialog mit vorbefuellten Signalen', () => {
    const component = TestBed.createComponent(AdminProductsComponent)
      .componentInstance;

    component.bearbeiteProdukt({
      ...beispielProdukt,
      repoUrl: 'https://gitlab.example.com/team/repo.git'
    });

    expect(component.editDialogOffen()).toBeTrue();
    expect(component.editProdukt()?.id).toBe('p1');
    expect(component.editName()).toBe('PortalCore-Test');
    expect(component.editBeschreibung()).toBe('Alte Beschreibung');
    expect(component.editRepoUrl()).toBe('https://gitlab.example.com/team/repo.git');
    expect(component.editRepoUrlFehler()).toBeNull();
  });

  it('bearbeiteProdukt befuellt leere Felder bei null-Werten', () => {
    const component = TestBed.createComponent(AdminProductsComponent)
      .componentInstance;

    component.bearbeiteProdukt({
      ...beispielProdukt,
      description: null,
      repoUrl: null
    });

    expect(component.editBeschreibung()).toBe('');
    expect(component.editRepoUrl()).toBe('');
  });

  it('speichereEdit warnt bei leerem Namen', async () => {
    const component = TestBed.createComponent(AdminProductsComponent)
      .componentInstance;

    component.bearbeiteProdukt(beispielProdukt);
    component.editName.set('   ');

    await component.speichereEdit();

    expect(toast.warning).toHaveBeenCalledWith('Name ist Pflichtfeld.');
    expect(products.update).not.toHaveBeenCalled();
  });

  it('speichereEdit setzt repoUrl-Fehler bei ungueltigem URL-Format', async () => {
    const component = TestBed.createComponent(AdminProductsComponent)
      .componentInstance;

    component.bearbeiteProdukt(beispielProdukt);
    component.editName.set('Neuer Name');
    component.editRepoUrl.set('kein-url');

    await component.speichereEdit();

    expect(component.editRepoUrlFehler()).toContain('http');
    expect(products.update).not.toHaveBeenCalled();
  });

  it('speichereEdit akzeptiert https://, ssh:// und git@ URLs', async () => {
    const component = TestBed.createComponent(AdminProductsComponent)
      .componentInstance;
    products.update.and.returnValue(
      Promise.resolve({ ...beispielProdukt, name: 'N' })
    );

    for (const url of [
      'https://gitlab.example.com/x.git',
      'ssh://git@gitlab.example.com/x.git',
      'git@gitlab.example.com:team/x.git'
    ]) {
      products.update.calls.reset();
      component.bearbeiteProdukt(beispielProdukt);
      component.editName.set('Neuer Name');
      component.editRepoUrl.set(url);
      await component.speichereEdit();
      expect(products.update).toHaveBeenCalledWith(
        'p1',
        jasmine.objectContaining({ repoUrl: url })
      );
    }
  });

  it('speichereEdit schickt Update, schliesst Dialog und ruft list neu', async () => {
    const component = TestBed.createComponent(AdminProductsComponent)
      .componentInstance;
    products.update.and.returnValue(
      Promise.resolve({
        id: 'p1',
        key: 'portalcore-test',
        name: 'Neuer Name',
        description: 'Neue Beschreibung',
        repoUrl: null
      })
    );

    component.bearbeiteProdukt(beispielProdukt);
    component.editName.set('Neuer Name');
    component.editBeschreibung.set('Neue Beschreibung');
    component.editRepoUrl.set('');

    await component.speichereEdit();

    expect(products.update).toHaveBeenCalledWith('p1', {
      name: 'Neuer Name',
      description: 'Neue Beschreibung',
      repoUrl: ''
    });
    expect(component.editDialogOffen()).toBeFalse();
    expect(component.editProdukt()).toBeNull();
    expect(products.list).toHaveBeenCalled();
    expect(toast.success).toHaveBeenCalled();
  });

  it('brecheEditAb schliesst Dialog und setzt Zustand zurueck', () => {
    const component = TestBed.createComponent(AdminProductsComponent)
      .componentInstance;

    component.bearbeiteProdukt(beispielProdukt);
    component.editRepoUrlFehler.set('err');
    component.brecheEditAb();

    expect(component.editDialogOffen()).toBeFalse();
    expect(component.editProdukt()).toBeNull();
    expect(component.editRepoUrlFehler()).toBeNull();
  });
});
