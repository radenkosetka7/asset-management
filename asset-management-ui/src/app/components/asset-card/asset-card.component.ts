import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AssetResponse, AssetStatus } from '../../models/asset.model';
@Component({
  selector: 'app-asset-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './asset-card.component.html',
  styleUrl: './asset-card.component.css'
})
export class AssetCardComponent implements OnChanges {
  @Input({ required: true }) asset!: AssetResponse;
  imageLoadFailed = false;

  constructor(private router: Router) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['asset']) {
      this.imageLoadFailed = false;
    }
  }

  navigateToDetail(): void {
    this.router.navigate(['/assets', this.asset.id]);
  }
  get statusClass(): string {
    const map: Record<AssetStatus, string> = {
      ACTIVE: 'status-active',
      IN_USE: 'status-in-use',
      DAMAGED: 'status-damaged',
      RETIRED: 'status-retired'
    };
    return map[this.asset.assetStatus] ?? 'status-active';
  }
  get statusLabel(): string {
    const map: Record<AssetStatus, string> = {
      ACTIVE: 'Active',
      IN_USE: 'In Use',
      DAMAGED: 'Damaged',
      RETIRED: 'Retired'
    };
    return map[this.asset.assetStatus] ?? this.asset.assetStatus;
  }
  get firstImage(): string | null {
    if (this.imageLoadFailed) return null;
    const images = this.asset.images;
    if (images && images.length > 0) {
      return this.imageUrl(images[0].fileName);
    }
    return null;
  }

  onImageError(): void {
    this.imageLoadFailed = true;
  }

  private imageUrl(fileName: string): string {
    return '/api/assets/images/file/' + encodeURIComponent(fileName);
  }
}
