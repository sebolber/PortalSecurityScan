import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiClient } from '../api/api-client.service';

export interface ProductView {
  readonly id: string;
  readonly key: string;
  readonly name: string;
  readonly description: string | null;
}

export interface ProductVersionView {
  readonly id: string;
  readonly productId: string;
  readonly version: string;
  readonly gitCommit: string | null;
  readonly releasedAt: string | null;
}

@Injectable({ providedIn: 'root' })
export class ProductsService {
  private readonly api = inject(ApiClient);

  list(): Promise<ProductView[]> {
    return firstValueFrom(this.api.get<ProductView[]>('/api/v1/products'));
  }

  versions(productId: string): Promise<ProductVersionView[]> {
    return firstValueFrom(
      this.api.get<ProductVersionView[]>(
        `/api/v1/products/${productId}/versions`
      )
    );
  }
}
