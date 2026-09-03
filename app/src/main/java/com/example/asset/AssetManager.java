package com.example.asset;

import java.util.ArrayList;
import java.util.List;

public class AssetManager {
    private final List<Asset> assets = new ArrayList<>();

    public AssetManager() {
        // Phase 15 Alignment: Purged hardcoded mock sample assets.
        // The asset library is populated dynamically from generated 3D files stored locally.
    }

    public void setAssets(List<Asset> loadedAssets) {
        assets.clear();
        if (loadedAssets != null) {
            assets.addAll(loadedAssets);
        }
    }

    public List<Asset> getAssets() { 
        return assets; 
    }

    public void addAsset(Asset a) {
        if (a != null && !containsAsset(a.getId())) {
            assets.add(0, a); // Add newest generated assets to the top
        }
    }

    public boolean removeAsset(String assetId) {
        if (assetId == null || assetId.trim().isEmpty()) {
            return false;
        }
        return assets.removeIf(a -> a.getId().equals(assetId));
    }

    public Asset getAssetById(String assetId) {
        if (assetId == null || assetId.trim().isEmpty()) {
            return null;
        }
        for (Asset a : assets) {
            if (a.getId().equals(assetId)) {
                return a;
            }
        }
        return null;
    }

    public boolean containsAsset(String assetId) {
        return getAssetById(assetId) != null;
    }

    public List<Asset> getAssetsByCategory(String category) {
        List<Asset> filtered = new ArrayList<>();
        if (category == null || category.trim().isEmpty() || "ALL".equalsIgnoreCase(category)) {
            return new ArrayList<>(assets);
        }
        for (Asset a : assets) {
            if (category.equalsIgnoreCase(a.getCategory())) {
                filtered.add(a);
            }
        }
        return filtered;
    }

    public List<Asset> searchAssets(String query) {
        List<Asset> results = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>(assets);
        }
        String q = query.toLowerCase().trim();
        for (Asset a : assets) {
            if ((a.getName() != null && a.getName().toLowerCase().contains(q)) ||
                (a.getCategory() != null && a.getCategory().toLowerCase().contains(q)) ||
                (a.getFormat() != null && a.getFormat().toLowerCase().contains(q))) {
                results.add(a);
            }
        }
        return results;
    }

    public void clearAssets() {
        assets.clear();
    }
}